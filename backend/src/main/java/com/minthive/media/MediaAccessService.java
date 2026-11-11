package com.minthive.media;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.entity.*;
import com.minthive.mapper.*;
import com.minthive.media.MediaLocator.Ref;
import com.minthive.service.ContentAccessService;
import com.minthive.util.MinioUtil;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** Authorization comes from persisted references, never from the response/request being rewritten. */
@Service
@RequiredArgsConstructor
public class MediaAccessService {
    private final MinioUtil storage;
    private final MediaLocator locator;
    private final ContentAccessService contentAccess;
    private final PostMapper posts;
    private final CommentMapper comments;
    private final MessageMapper messages;
    private final UserMapper users;
    private final CircleMapper circles;
    private final BannerMapper banners;
    private final ObjectMapper json;

    public void requireActiveViewer(Long viewer) {
        if (viewer == null) return;
        User user = users.selectById(viewer);
        if (!active(user)) throw new BusinessException(ResultCode.FORBIDDEN, "媒体访问账户不可用");
    }

    private boolean active(User user) {
        return user != null && !Objects.equals(user.getDeleted(), 1)
                && Objects.equals(user.getStatus(), 1) && Objects.equals(user.getCancelStatus(), 0);
    }

    public MinioUtil.Metadata metadata(Ref ref) {
        try {
            return storage.stat(ref);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code()) || "NoSuchObject".equals(e.errorResponse().code())) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            throw new BusinessException(ResultCode.SERVER_ERROR, "媒体存储不可用");
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "媒体存储不可用或私有桶检查失败");
        }
    }

    public MinioUtil.Metadata requireReadable(Ref ref, Long viewer) {
        requireActiveViewer(viewer);
        MinioUtil.Metadata meta = metadata(ref);
        if (!canRead(ref, meta, viewer)) throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该媒体");
        return meta;
    }

    /** Reading a private reference is not permission to republish/forward it into a different audience. */
    public void requireReference(Ref ref, Long viewer) {
        MinioUtil.Metadata meta = requireReadable(ref, viewer);
        if (viewer != null && !ref.legacy() && viewer.equals(meta.ownerId())) return;
        if (!canRead(ref, meta, null)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不可复用他人的私有媒体；旧私有媒体需先迁移归属");
        }
    }

    public void requireDeletable(Ref ref, Long viewer) {
        if (viewer == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        requireActiveViewer(viewer);
        if (ref.legacy()) throw new BusinessException(ResultCode.FORBIDDEN, "旧媒体归属不明确，禁止删除");
        Long owner = metadata(ref).ownerId();
        if (owner == null || !(owner.equals(viewer) || contentAccess.isAdmin(viewer))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的媒体");
        }
    }

    /**
     * Only the uploader's references can delegate access to new objects. A reader cannot publish
     * someone else's private/chat URL as an avatar or public post to turn it into a public capability.
     * All authoritative references must be readable (intersection, not union). For ownerless legacy
     * objects all exact persisted references are authoritative; conflicts therefore fail closed.
     */
    public boolean canRead(Ref ref, MinioUtil.Metadata meta, Long viewer) {
        Long owner = ref.legacy() ? null : meta.ownerId();
        if (!ref.legacy() && owner == null) return false;
        if (viewer != null && viewer.equals(owner)) return true;
        List<String> aliases = locator.aliases(ref);
        boolean found = false;

        for (Post post : posts.selectList(mediaQuery(aliases, "images", "video"))) {
            if (!authoritative(owner, post.getUserId()) || Objects.equals(post.getDeleted(), 1)
                    || !(matches(aliases, post.getVideo()) || matchesImages(aliases, post.getImages()))) continue;
            found = true;
            if (!contentAccess.canRead(post, viewer)) return false;
        }
        for (Comment comment : comments.selectList(mediaQuery(aliases, "images"))) {
            if (!authoritative(owner, comment.getUserId()) || Objects.equals(comment.getDeleted(), 1)
                    || !matchesImages(aliases, comment.getImages())) continue;
            found = true;
            if (!contentAccess.canRead(posts.selectById(comment.getPostId()), viewer)) return false;
        }
        QueryWrapper<Message> messageQuery = exactQuery(aliases, "content");
        messageQuery.eq("msg_type", 1);
        for (Message message : messages.selectList(messageQuery)) {
            if (!authoritative(owner, message.getFromUserId()) || Objects.equals(message.getDeleted(), 1)
                    || !Objects.equals(message.getMsgType(), 1) || !matches(aliases, message.getContent())) continue;
            found = true;
            if (viewer == null || !(viewer.equals(message.getFromUserId()) || viewer.equals(message.getToUserId()))) return false;
        }
        for (User user : users.selectList(exactQuery(aliases, "avatar"))) {
            if (!authoritative(owner, user.getId()) || !matches(aliases, user.getAvatar()) || Objects.equals(user.getDeleted(), 1)) continue;
            found = true;
            if (!active(user)) return false;
        }
        for (Circle circle : circles.selectList(exactQuery(aliases, "avatar", "banner"))) {
            if (!authoritative(owner, circle.getOwnerId()) || Objects.equals(circle.getDeleted(), 1)
                    || !(matches(aliases, circle.getAvatar()) || matches(aliases, circle.getBanner()))) continue;
            found = true;
            if (!contentAccess.canReadCircle(circle.getId(), viewer)) return false;
        }
        for (Banner banner : banners.selectList(exactQuery(aliases, "image_url"))) {
            // Banner writes are administrator-only and have no author column. Normal users' objects cannot be promoted here.
            if (!matches(aliases, banner.getImageUrl()) || (owner != null && !contentAccess.isAdmin(owner))) continue;
            found = true;
            if (!"ACTIVE".equals(banner.getStatus()) && !contentAccess.isAdmin(viewer)) return false;
        }
        return found;
    }

    private boolean authoritative(Long owner, Long author) { return owner == null || owner.equals(author); }
    private boolean matches(List<String> aliases, String value) { return value != null && aliases.contains(value); }

    private boolean matchesImages(List<String> aliases, String value) {
        if (value == null) return false;
        try {
            JsonNode node = json.readTree(value);
            if (node == null || !node.isArray()) return false;
            for (JsonNode item : node) if (item.isTextual() && matches(aliases, item.textValue())) return true;
        } catch (Exception ignored) {
            // Malformed historical JSON never grants access.
        }
        return false;
    }

    private <T> QueryWrapper<T> exactQuery(List<String> aliases, String... columns) {
        QueryWrapper<T> query = new QueryWrapper<>();
        query.and(q -> {
            for (String column : columns) q.or().in(column, aliases);
        });
        return query;
    }

    private <T> QueryWrapper<T> mediaQuery(List<String> aliases, String jsonColumn, String... scalarColumns) {
        QueryWrapper<T> query = new QueryWrapper<>();
        query.and(q -> {
            for (String column : scalarColumns) q.or().in(column, aliases);
            for (String alias : aliases) {
                // Column names are fixed server literals; values always use MyBatis bound parameters.
                q.or().apply("JSON_CONTAINS(IF(JSON_VALID(" + jsonColumn + "), " + jsonColumn
                        + ", '[]'), JSON_QUOTE({0}))", alias);
            }
        });
        return query;
    }
}
