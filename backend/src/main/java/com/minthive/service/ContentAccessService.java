package com.minthive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Circle;
import com.minthive.entity.CircleUser;
import com.minthive.entity.Comment;
import com.minthive.entity.Follow;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.mapper.CircleMapper;
import com.minthive.mapper.CircleUserMapper;
import com.minthive.mapper.CommentMapper;
import com.minthive.mapper.FollowMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentAccessService {
    private final PostMapper postMapper;
    private final CircleMapper circleMapper;
    private final CircleUserMapper circleUserMapper;
    private final CommentMapper commentMapper;
    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    public Long currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }

    public boolean isAdmin(Long viewerId) {
        if (viewerId == null) return false;
        User user = userMapper.selectById(viewerId);
        return user != null && Objects.equals(user.getStatus(), 1)
                && Objects.equals(user.getCancelStatus(), 0)
                && Objects.equals(user.getRole(), Constants.ROLE_ADMIN);
    }

    public LambdaQueryWrapper<Post> visiblePosts(Long viewerId) {
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<>();
        if (isAdmin(viewerId)) return query;
        long viewer = viewerId == null ? -1L : viewerId;
        return query.apply("""
                (post.user_id = {0} OR (
                    post.audit_status = 1
                    AND (post.visibility = 0 OR (post.visibility = 1 AND EXISTS (
                        SELECT 1 FROM follow f WHERE f.user_id = {0} AND f.follow_user_id = post.user_id)))
                    AND (post.circle_id IS NULL OR EXISTS (
                        SELECT 1 FROM circle c WHERE c.id = post.circle_id AND c.deleted = 0 AND c.status = 1
                        AND (c.type = 0 OR c.owner_id = {0} OR EXISTS (
                            SELECT 1 FROM circle_user cu WHERE cu.circle_id = c.id
                            AND cu.user_id = {0} AND cu.audit_status = 1))))))
                AND (post.share_post_id IS NULL OR EXISTS (
                    SELECT 1 FROM post original WHERE original.id = post.share_post_id
                    AND original.deleted = 0 AND original.audit_status = 1 AND original.visibility = 0
                    AND original.share_post_id IS NULL
                    AND (original.circle_id IS NULL OR EXISTS (
                        SELECT 1 FROM circle oc WHERE oc.id = original.circle_id
                        AND oc.deleted = 0 AND oc.status = 1 AND oc.type = 0))))
                """, viewer);
    }

    public boolean canRead(Post post, Long viewerId) {
        return canRead(post, viewerId, new HashSet<>());
    }

    private boolean canRead(Post post, Long viewerId, Set<Long> visited) {
        if (post == null || Objects.equals(post.getDeleted(), 1)) return false;
        if (post.getId() != null && !visited.add(post.getId())) return false;
        if (post.getSharePostId() != null && !canRead(postMapper.selectById(post.getSharePostId()), viewerId, visited)) {
            return false;
        }
        if (viewerId != null && (Objects.equals(post.getUserId(), viewerId) || isAdmin(viewerId))) return true;
        if (!Objects.equals(post.getAuditStatus(), Constants.AUDIT_PASS)) return false;
        boolean audience = Objects.equals(post.getVisibility(), 0)
                || (Objects.equals(post.getVisibility(), 1) && viewerId != null && followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, viewerId)
                        .eq(Follow::getFollowUserId, post.getUserId())) > 0);
        return audience && canReadCircle(post.getCircleId(), viewerId);
    }

    public void requireReadable(Post post, Long viewerId) {
        if (!canRead(post, viewerId)) throw new BusinessException(ResultCode.POST_NOT_EXISTS);
    }

    public boolean canReadCircle(Long circleId, Long viewerId) {
        if (circleId == null) return true;
        Circle circle = circleMapper.selectById(circleId);
        if (circle == null || !Objects.equals(circle.getStatus(), Constants.CIRCLE_STATUS_NORMAL)) return false;
        return Objects.equals(circle.getType(), 0) || isCircleMember(circle, viewerId) || isAdmin(viewerId);
    }

    /**
     * 判断查看者能否看到圈子详情中的受限字段（公告、活动量）
     *
     * @param circle   圈子实体
     * @param viewerId 查看者用户ID（可为空，表示匿名）
     * @return true=公开圈或查看者是该圈成员/圈主/管理员；false=私密圈且无成员身份
     * @note 列表、搜索、推荐接口与详情接口必须使用同一判定，避免私密圈公告经公开列表泄露
     */
    public boolean canViewCircleDetails(Circle circle, Long viewerId) {
        if (circle == null) return false;
        if (!Objects.equals(circle.getType(), Constants.CIRCLE_TYPE_PRIVATE)) return true;
        return isCircleMember(circle, viewerId) || isAdmin(viewerId);
    }

    public void requireCircleReadable(Long circleId, Long viewerId) {
        if (!canReadCircle(circleId, viewerId)) throw new BusinessException(ResultCode.FORBIDDEN, "无权查看圈内内容");
    }

    public void requireCanPublish(Long circleId, Long userId) {
        if (circleId == null) return;
        Circle circle = circleMapper.selectById(circleId);
        if (circle == null || !Objects.equals(circle.getStatus(), Constants.CIRCLE_STATUS_NORMAL)) {
            throw new BusinessException(ResultCode.CIRCLE_NOT_EXISTS);
        }
        if (!isCircleMember(circle, userId)) throw new BusinessException(ResultCode.FORBIDDEN, "请先加入圈子并通过审核");
    }

    public boolean canReadComment(Long commentId, Long viewerId) {
        Comment comment = commentId == null ? null : commentMapper.selectById(commentId);
        return comment != null && canRead(postMapper.selectById(comment.getPostId()), viewerId);
    }

    public void requireCommentReadable(Long commentId, Long viewerId) {
        if (!canReadComment(commentId, viewerId)) throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
    }

    public boolean isPubliclyShareable(Post post) {
        if (post == null || post.getSharePostId() != null || !Objects.equals(post.getAuditStatus(), Constants.AUDIT_PASS)
                || !Objects.equals(post.getVisibility(), 0) || Objects.equals(post.getDeleted(), 1)) return false;
        return canReadCircle(post.getCircleId(), null);
    }

    private boolean isCircleMember(Circle circle, Long userId) {
        return userId != null && (Objects.equals(circle.getOwnerId(), userId)
                || circleUserMapper.selectCount(new LambdaQueryWrapper<CircleUser>()
                .eq(CircleUser::getCircleId, circle.getId()).eq(CircleUser::getUserId, userId)
                .eq(CircleUser::getAuditStatus, 1)) > 0);
    }
}
