package com.minthive.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.*;
import com.minthive.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolService {

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final CircleMapper circleMapper;
    private final CircleUserMapper circleUserMapper;
    private final CommentMapper commentMapper;
    private final LikeCollectMapper likeCollectMapper;
    private final FollowMapper followMapper;
    private final SystemMsgMapper systemMsgMapper;
    private final AnnouncementMapper announcementMapper;
    private final CircleCategoryMapper circleCategoryMapper;
    private final MessageMapper messageMapper;

    public ToolResult execute(IntentParser.ParsedIntent intent) {
        return switch (intent.getType()) {
            case GET_POST -> getPostDetail((Long) intent.getParams().get("id"));
            case GET_CIRCLE -> getCircleDetail((Long) intent.getParams().get("id"));
            case GET_USER -> getUserDetail((Long) intent.getParams().get("id"));
            case GET_STATS -> getPlatformStats();
            case SEARCH_POSTS -> searchPosts((String) intent.getParams().get("keyword"));
            case GET_TRENDING -> getTrendingPosts();
            case GET_LATEST_POSTS -> getLatestPosts();
            case GET_RECOMMEND -> getRecommendPosts();
            case GET_NOTIFICATIONS -> getNotifications(toLong(intent.getParams().get("userId")));
            case GET_ANNOUNCEMENTS -> getAnnouncements();
            case GET_FOLLOWERS -> getFollowers(toLong(intent.getParams().get("userId")));
            case GET_FOLLOWING -> getFollowing(toLong(intent.getParams().get("userId")));
            case GET_POST_LIKES -> getPostLikes(toLong(intent.getParams().get("postId")));
            case GET_COLLECTIONS -> getCollections(toLong(intent.getParams().get("userId")));
            case SEARCH_POSTS_BY_TAGS -> searchPostsByTags((String) intent.getParams().get("keyword"));
            case SEARCH_CIRCLES_BY_CATEGORY -> searchCirclesByCategory((String) intent.getParams().get("keyword"));
            case GET_MESSAGES -> getMessages(toLong(intent.getParams().get("userId")));
            case GET_MESSAGE_CENTER -> getMessageCenter(toLong(intent.getParams().get("userId")));
            case GET_NEW_USERS -> getNewUsers();
            case GET_CIRCLE_POSTS -> getCirclePosts(toLong(intent.getParams().get("circleId")));
            case GET_HOT_CIRCLES -> getHotCircles();
            case GET_USER_POSTS -> getUserPosts(toLong(intent.getParams().get("userId")));
            case SEARCH_CIRCLES -> searchCircles((String) intent.getParams().get("keyword"));
            case SEARCH_USERS -> searchUsers((String) intent.getParams().get("keyword"));
            case GET_COMMENTS -> getComments(toLong(intent.getParams().get("postId")));
            case GET_DAILY_STATS -> getDailyStats();
            default -> null;
        };
    }

    private Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) return Long.parseLong(s);
        return (Long) v;
    }

    private ToolResult getPostDetail(Long id) {
        try {
            Post post = postMapper.selectById(id);
            if (post == null) {
                return ToolResult.failure("get_post", "未找到ID为 " + id + " 的帖子");
            }
            User author = userMapper.selectById(post.getUserId());
            long likeCount = likeCollectMapper.selectCount(
                    new LambdaQueryWrapper<LikeCollect>()
                            .eq(LikeCollect::getTargetId, id)
                            .eq(LikeCollect::getType, 1));
            long commentCount = commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getPostId, id));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", post.getId());
            data.put("content", post.getContent());
            data.put("images", post.getImages());
            data.put("authorNickname", author != null ? author.getNickname() : "未知");
            data.put("authorAvatar", author != null ? author.getAvatar() : "");
            data.put("likeCount", likeCount);
            data.put("commentCount", commentCount);
            data.put("createTime", post.getCreateTime() != null ? post.getCreateTime().toString() : "");

            String mediaTag = postMediaIndicator(post);
            String summary = String.format("找到帖子《%s》%s——作者：%s，点赞 %d，评论 %d",
                    truncate(post.getContent(), 50),
                    mediaTag,
                    author != null ? author.getNickname() : "未知",
                    likeCount, commentCount);

            List<NavigationAction> nav = List.of(
                    new NavigationAction("查看帖子", "/post/" + id, "post", truncate(post.getContent(), 30))
            );

            return ToolResult.builder()
                    .success(true).toolName("get_post")
                    .toolDisplayName("帖子详情")
                    .data(data).dataSummary(summary)
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getPostDetail error: ", e);
            return ToolResult.failure("get_post", "查询帖子失败");
        }
    }

    private ToolResult getCircleDetail(Long id) {
        try {
            Circle circle = circleMapper.selectById(id);
            if (circle == null) {
                return ToolResult.failure("get_circle", "未找到ID为 " + id + " 的圈子");
            }
            long memberCount = circleUserMapper.selectCount(
                    new LambdaQueryWrapper<CircleUser>().eq(CircleUser::getCircleId, id));
            long postCount = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>().eq(Post::getCircleId, id).eq(Post::getDeleted, 0));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", circle.getId());
            data.put("name", circle.getName());
            data.put("intro", circle.getIntro());
            data.put("memberCount", memberCount);
            data.put("postCount", postCount);
            data.put("avatar", circle.getAvatar());

            String summary = String.format("圈子「%s」%s—— %s，成员 %d 人，帖子 %d 篇",
                    circle.getName(),
                    circleMediaIndicator(circle),
                    circle.getIntro() != null ? truncate(circle.getIntro(), 40) : "暂无简介",
                    memberCount, postCount);

            List<NavigationAction> nav = List.of(
                    new NavigationAction("进入圈子", "/circle/" + id, "circle", circle.getName())
            );

            return ToolResult.builder()
                    .success(true).toolName("get_circle")
                    .toolDisplayName("圈子详情")
                    .data(data).dataSummary(summary)
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getCircleDetail error: ", e);
            return ToolResult.failure("get_circle", "查询圈子失败");
        }
    }

    private ToolResult getUserDetail(Long id) {
        try {
            User user = userMapper.selectById(id);
            if (user == null) {
                return ToolResult.failure("get_user", "未找到ID为 " + id + " 的用户");
            }
            long postCount = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>().eq(Post::getUserId, id));
            long fanCount = 0L;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", user.getId());
            data.put("nickname", user.getNickname());
            data.put("bio", user.getBio());
            data.put("avatar", user.getAvatar());
            data.put("postCount", postCount);
            data.put("interestTags", user.getInterestTags());

            String summary = String.format("用户「%s」—— %s，发了 %d 篇帖子",
                    user.getNickname(),
                    user.getBio() != null ? truncate(user.getBio(), 30) : "暂无简介",
                    postCount);

            List<NavigationAction> nav = List.of(
                    new NavigationAction("查看主页", "/profile/" + id, "user", user.getNickname())
            );

            return ToolResult.builder()
                    .success(true).toolName("get_user")
                    .toolDisplayName("用户信息")
                    .data(data).dataSummary(summary)
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getUserDetail error: ", e);
            return ToolResult.failure("get_user", "查询用户失败");
        }
    }

    private ToolResult getPlatformStats() {
        try {
            long userCount = userMapper.selectCount(new QueryWrapper<>());
            long postCount = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>().eq(Post::getDeleted, 0));
            long commentCount = commentMapper.selectCount(new QueryWrapper<>());
            long circleCount = circleMapper.selectCount(
                    new LambdaQueryWrapper<Circle>().eq(Circle::getDeleted, 0));
            long todayPostCount = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getDeleted, 0)
                            .ge(Post::getCreateTime, LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userCount", userCount);
            data.put("postCount", postCount);
            data.put("commentCount", commentCount);
            data.put("circleCount", circleCount);
            data.put("todayPostCount", todayPostCount);

            String summary = String.format(
                    "📊 MintHive 平台数据：\n• 总用户：%d 人\n• 总帖子：%d 篇\n• 总评论：%d 条\n• 兴趣圈子：%d 个\n• 今日新帖：%d 篇",
                    userCount, postCount, commentCount, circleCount, todayPostCount);

            return ToolResult.builder()
                    .success(true).toolName("get_stats")
                    .toolDisplayName("平台数据")
                    .data(data).dataSummary(summary)
                    .navigation(List.of()).build();
        } catch (Exception e) {
            log.error("[AiTool] getPlatformStats error: ", e);
            return ToolResult.failure("get_stats", "查询平台数据失败");
        }
    }

    private ToolResult searchPosts(String keyword) {
        try {
            List<Post> posts = postMapper.selectPage(
                    new Page<>(1, 5),
                    new LambdaQueryWrapper<Post>()
                            .like(Post::getContent, keyword)
                            .eq(Post::getDeleted, 0)
                            .orderByDesc(Post::getCreateTime)
            ).getRecords();

            if (posts.isEmpty()) {
                return ToolResult.failure("search_posts", "没有找到包含「" + keyword + "」的帖子");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("找到 ").append(posts.size()).append(" 篇相关帖子：\n");
            for (int i = 0; i < posts.size(); i++) {
                Post p = posts.get(i);
                sb.append("• ").append(truncate(p.getContent(), 60)).append(postMediaIndicator(p)).append("\n");
                nav.add(new NavigationAction("查看帖子" + (i + 1), "/post/" + p.getId(), "post",
                        truncate(p.getContent(), 30)));
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("keyword", keyword);
            data.put("count", posts.size());
            data.put("posts", posts.stream().map(p -> Map.of(
                    "id", p.getId(),
                    "content", truncate(p.getContent(), 60),
                    "createTime", p.getCreateTime() != null ? p.getCreateTime().toString() : ""
            )).toList());

            return ToolResult.builder()
                    .success(true).toolName("search_posts")
                    .toolDisplayName("搜索结果")
                    .data(data).dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] searchPosts error: ", e);
            return ToolResult.failure("search_posts", "搜索帖子失败");
        }
    }

    private ToolResult getTrendingPosts() {
        try {
            Page<Post> page = postMapper.selectHotFeed(new Page<>(1, 5));
            List<Post> posts = page.getRecords();

            if (posts.isEmpty()) {
                return ToolResult.failure("get_trending", "暂无热门帖子");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("🔥 热门帖子 Top ").append(posts.size()).append("：\n");
            for (int i = 0; i < posts.size(); i++) {
                Post p = posts.get(i);
                sb.append("• ").append(truncate(p.getContent(), 60)).append(postMediaIndicator(p)).append("\n");
                nav.add(new NavigationAction("查看热门" + (i + 1), "/post/" + p.getId(), "post",
                        truncate(p.getContent(), 30)));
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", posts.size());
            data.put("posts", posts.stream().map(p -> Map.of(
                    "id", p.getId(),
                    "content", truncate(p.getContent(), 60),
                    "createTime", p.getCreateTime() != null ? p.getCreateTime().toString() : ""
            )).toList());

            return ToolResult.builder()
                    .success(true).toolName("get_trending")
                    .toolDisplayName("热门帖子")
                    .data(data).dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getTrendingPosts error: ", e);
            return ToolResult.failure("get_trending", "查询热门帖子失败");
        }
    }

    private ToolResult getLatestPosts() {
        try {
            List<Post> posts = postMapper.selectPage(
                    new Page<>(1, 5),
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getDeleted, 0)
                            .eq(Post::getAuditStatus, 1)
                            .eq(Post::getVisibility, 0)
                            .orderByDesc(Post::getCreateTime)
            ).getRecords();

            if (posts.isEmpty()) {
                return ToolResult.failure("get_latest_posts", "暂无最新帖子");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("📰 最新帖子 Top ").append(posts.size()).append("：\n");
            for (int i = 0; i < posts.size(); i++) {
                Post p = posts.get(i);
                sb.append("• ").append(truncate(p.getContent(), 60)).append(postMediaIndicator(p)).append("\n");
                nav.add(new NavigationAction("查看帖子" + (i + 1), "/post/" + p.getId(), "post",
                        truncate(p.getContent(), 30)));
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", posts.size());
            data.put("posts", posts.stream().map(p -> Map.of(
                    "id", p.getId(),
                    "content", truncate(p.getContent(), 60),
                    "createTime", p.getCreateTime() != null ? p.getCreateTime().toString() : ""
            )).toList());

            return ToolResult.builder()
                    .success(true).toolName("get_latest_posts")
                    .toolDisplayName("最新帖子")
                    .data(data).dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getLatestPosts error: ", e);
            return ToolResult.failure("get_latest_posts", "查询最新帖子失败");
        }
    }

    private ToolResult getRecommendPosts() {
        try {
            Page<Post> page = postMapper.selectRecommendFeed(new Page<>(1, 5));
            List<Post> posts = page.getRecords();

            if (posts.isEmpty()) {
                return ToolResult.failure("get_recommend", "暂无推荐帖子");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("⭐ 推荐帖子 Top ").append(posts.size()).append("：\n");
            for (int i = 0; i < posts.size(); i++) {
                Post p = posts.get(i);
                sb.append("• ").append(truncate(p.getContent(), 60)).append(postMediaIndicator(p)).append("\n");
                nav.add(new NavigationAction("查看帖子" + (i + 1), "/post/" + p.getId(), "post",
                        truncate(p.getContent(), 30)));
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", posts.size());
            data.put("posts", posts.stream().map(p -> Map.of(
                    "id", p.getId(),
                    "content", truncate(p.getContent(), 60),
                    "createTime", p.getCreateTime() != null ? p.getCreateTime().toString() : ""
            )).toList());

            return ToolResult.builder()
                    .success(true).toolName("get_recommend")
                    .toolDisplayName("推荐帖子")
                    .data(data).dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getRecommendPosts error: ", e);
            return ToolResult.failure("get_recommend", "查询推荐帖子失败");
        }
    }

    private ToolResult getNotifications(Long userId) {
        try {
            if (userId == null) {
                return ToolResult.failure("get_notifications", "请指定用户ID");
            }
            List<SystemMsg> msgs = systemMsgMapper.selectPage(
                    new Page<>(1, 10),
                    new LambdaQueryWrapper<SystemMsg>()
                            .eq(SystemMsg::getUserId, userId)
                            .orderByDesc(SystemMsg::getCreateTime)
            ).getRecords();

            if (msgs.isEmpty()) {
                return ToolResult.failure("get_notifications", "用户暂无通知消息");
            }

            long unreadCount = msgs.stream().filter(m -> m.getIsRead() == 0).count();
            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("🔔 用户 ").append(userId).append(" 的通知（共 ").append(msgs.size()).append(" 条，未读 ").append(unreadCount).append(" 条）：\n");
            Map<Integer, String> typeNames = Map.of(1, "点赞", 2, "评论", 3, "私信", 4, "圈子公告", 5, "系统公告");
            int navIdx = 0;
            for (SystemMsg m : msgs) {
                String typeName = typeNames.getOrDefault(m.getMsgType(), "其他");
                String readTag = (m.getIsRead() == 0 ? "【未读】" : "");
                sb.append("• ").append(readTag).append("[").append(typeName).append("] ").append(truncate(m.getContent(), 50)).append("\n");
                if (m.getTargetId() != null) {
                    if (m.getMsgType() == 1 || m.getMsgType() == 2) {
                        nav.add(new NavigationAction("查看帖子", "/post/" + m.getTargetId(), "post", truncate(m.getContent(), 30)));
                    } else if (m.getMsgType() == 4) {
                        nav.add(new NavigationAction("查看圈子", "/circle/" + m.getTargetId(), "circle", truncate(m.getContent(), 30)));
                    }
                }
            }

            return ToolResult.builder()
                    .success(true).toolName("get_notifications")
                    .toolDisplayName("用户通知")
                    .data(Map.of("userId", userId, "count", msgs.size(), "unreadCount", unreadCount))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getNotifications error: ", e);
            return ToolResult.failure("get_notifications", "查询通知失败");
        }
    }

    private ToolResult getAnnouncements() {
        try {
            List<Announcement> announcements = announcementMapper.selectList(
                    new LambdaQueryWrapper<Announcement>()
                            .eq(Announcement::getStatus, 1)
                            .orderByDesc(Announcement::getPublishTime)
                            .last("LIMIT 5"));

            if (announcements.isEmpty()) {
                return ToolResult.failure("get_announcements", "暂无系统公告");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("📢 平台公告（共 ").append(announcements.size()).append(" 条）：\n");
            for (int i = 0; i < announcements.size(); i++) {
                Announcement a = announcements.get(i);
                sb.append("• ").append(a.getTitle()).append("：").append(truncate(a.getContent(), 60)).append("\n");
                nav.add(new NavigationAction(a.getTitle(), "/chat?tab=announce", "announce", truncate(a.getContent(), 40)));
            }

            return ToolResult.builder()
                    .success(true).toolName("get_announcements")
                    .toolDisplayName("平台公告")
                    .data(Map.of("count", announcements.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getAnnouncements error: ", e);
            return ToolResult.failure("get_announcements", "查询公告失败");
        }
    }

    private ToolResult getFollowers(Long userId) {
        try {
            if (userId == null) {
                return ToolResult.failure("get_followers", "请指定用户ID");
            }
            List<Follow> follows = followMapper.selectList(
                    new LambdaQueryWrapper<Follow>()
                            .eq(Follow::getFollowUserId, userId)
                            .orderByDesc(Follow::getCreateTime)
                            .last("LIMIT 10"));

            if (follows.isEmpty()) {
                return ToolResult.failure("get_followers", "该用户暂无粉丝");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("👥 用户 ").append(userId).append(" 的粉丝（共 ").append(follows.size()).append(" 人）：\n");
            for (int i = 0; i < follows.size(); i++) {
                Follow f = follows.get(i);
                User u = userMapper.selectById(f.getUserId());
                String name = u != null ? u.getNickname() : ("用户" + f.getUserId());
                sb.append("• ").append(name).append("\n");
                nav.add(new NavigationAction("查看主页", "/profile/" + f.getUserId(), "user", name));
            }

            return ToolResult.builder()
                    .success(true).toolName("get_followers")
                    .toolDisplayName("粉丝列表")
                    .data(Map.of("userId", userId, "count", follows.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getFollowers error: ", e);
            return ToolResult.failure("get_followers", "查询粉丝失败");
        }
    }

    private ToolResult getFollowing(Long userId) {
        try {
            if (userId == null) {
                return ToolResult.failure("get_following", "请指定用户ID");
            }
            List<Follow> follows = followMapper.selectList(
                    new LambdaQueryWrapper<Follow>()
                            .eq(Follow::getUserId, userId)
                            .orderByDesc(Follow::getCreateTime)
                            .last("LIMIT 10"));

            if (follows.isEmpty()) {
                return ToolResult.failure("get_following", "该用户暂无关注的人");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("👤 用户 ").append(userId).append(" 的关注（共 ").append(follows.size()).append(" 人）：\n");
            for (int i = 0; i < follows.size(); i++) {
                Follow f = follows.get(i);
                User u = userMapper.selectById(f.getFollowUserId());
                String name = u != null ? u.getNickname() : ("用户" + f.getFollowUserId());
                sb.append("• ").append(name).append("\n");
                nav.add(new NavigationAction("查看主页", "/profile/" + f.getFollowUserId(), "user", name));
            }

            return ToolResult.builder()
                    .success(true).toolName("get_following")
                    .toolDisplayName("关注列表")
                    .data(Map.of("userId", userId, "count", follows.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getFollowing error: ", e);
            return ToolResult.failure("get_following", "查询关注列表失败");
        }
    }

    private ToolResult getPostLikes(Long postId) {
        try {
            if (postId == null) {
                return ToolResult.failure("get_post_likes", "请指定帖子ID");
            }
            Post post = postMapper.selectById(postId);
            if (post == null) {
                return ToolResult.failure("get_post_likes", "未找到该帖子");
            }
            List<LikeCollect> likes = likeCollectMapper.selectList(
                    new LambdaQueryWrapper<LikeCollect>()
                            .eq(LikeCollect::getTargetId, postId)
                            .eq(LikeCollect::getType, 1)
                            .orderByDesc(LikeCollect::getCreateTime)
                            .last("LIMIT 10"));

            if (likes.isEmpty()) {
                return ToolResult.failure("get_post_likes", "该帖子暂无点赞");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("❤️ 帖子《").append(truncate(post.getContent(), 30)).append("》的点赞用户（共 ").append(likes.size()).append(" 人）：\n");
            for (int i = 0; i < likes.size(); i++) {
                LikeCollect lc = likes.get(i);
                User u = userMapper.selectById(lc.getUserId());
                String name = u != null ? u.getNickname() : ("用户" + lc.getUserId());
                sb.append("• ").append(name).append("\n");
                nav.add(new NavigationAction("查看用户" + (i + 1), "/profile/" + lc.getUserId(), "user", name));
            }

            return ToolResult.builder()
                    .success(true).toolName("get_post_likes")
                    .toolDisplayName("点赞列表")
                    .data(Map.of("postId", postId, "count", likes.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getPostLikes error: ", e);
            return ToolResult.failure("get_post_likes", "查询点赞失败");
        }
    }

    private ToolResult getCollections(Long userId) {
        try {
            if (userId == null) {
                return ToolResult.failure("get_collections", "请指定用户ID");
            }
            List<LikeCollect> collects = likeCollectMapper.selectList(
                    new LambdaQueryWrapper<LikeCollect>()
                            .eq(LikeCollect::getUserId, userId)
                            .eq(LikeCollect::getType, 3)
                            .orderByDesc(LikeCollect::getCreateTime)
                            .last("LIMIT 10"));

            if (collects.isEmpty()) {
                return ToolResult.failure("get_collections", "该用户暂无收藏");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("📑 用户 ").append(userId).append(" 的收藏（共 ").append(collects.size()).append(" 篇）：\n");
            for (int i = 0; i < collects.size(); i++) {
                LikeCollect lc = collects.get(i);
                Post p = postMapper.selectById(lc.getTargetId());
                String content = p != null ? truncate(p.getContent(), 50) : ("帖子" + lc.getTargetId());
                sb.append("• ").append(content).append("\n");
                if (p != null) {
                    nav.add(new NavigationAction("查看帖子" + (i + 1), "/post/" + p.getId(), "post", truncate(p.getContent(), 30)));
                }
            }

            return ToolResult.builder()
                    .success(true).toolName("get_collections")
                    .toolDisplayName("用户收藏")
                    .data(Map.of("userId", userId, "count", collects.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getCollections error: ", e);
            return ToolResult.failure("get_collections", "查询收藏失败");
        }
    }

    private ToolResult getMessages(Long userId) {
        try {
            if (userId == null) {
                return ToolResult.failure("get_messages", "无法获取当前用户信息");
            }
            List<Message> msgs = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getDeleted, 0)
                            .and(w -> w.eq(Message::getFromUserId, userId)
                                    .or().eq(Message::getToUserId, userId))
                            .orderByDesc(Message::getCreateTime)
                            .last("LIMIT 10"));

            if (msgs.isEmpty()) {
                return ToolResult.failure("get_messages", "该用户暂无私信消息");
            }

            long unreadCount = msgs.stream().filter(m -> m.getIsRead() == 0
                    && m.getToUserId().equals(userId)).count();
            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("💬 用户 ").append(userId).append(" 的消息（共 ").append(msgs.size()).append(" 条，未读 ").append(unreadCount).append(" 条）：\n");
            for (int i = 0; i < msgs.size(); i++) {
                Message m = msgs.get(i);
                User sender = userMapper.selectById(m.getFromUserId());
                String senderName = sender != null ? sender.getNickname() : ("用户" + m.getFromUserId());
                boolean isIncoming = m.getToUserId().equals(userId);
                String direction = isIncoming ? "←" : "→";
                String readTag = (isIncoming && m.getIsRead() == 0 ? "【未读】" : "");
                sb.append("• ").append(direction).append(" ").append(senderName).append(readTag).append("：").append(truncate(m.getContent(), 50)).append("\n");
                Long chatPartnerId = isIncoming ? m.getFromUserId() : m.getToUserId();
                nav.add(new NavigationAction("打开对话", "/chat/" + chatPartnerId, "chat", senderName));
            }

            return ToolResult.builder()
                    .success(true).toolName("get_messages")
                    .toolDisplayName("用户私信")
                    .data(Map.of("userId", userId, "count", msgs.size(), "unreadCount", unreadCount))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getMessages error: ", e);
            return ToolResult.failure("get_messages", "查询私信失败");
        }
    }

    private ToolResult getMessageCenter(Long userId) {
        try {
            if (userId == null) {
                return ToolResult.failure("get_message_center", "无法获取当前用户信息");
            }

            // 1. 私信
            List<Message> msgs = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getDeleted, 0)
                            .and(w -> w.eq(Message::getFromUserId, userId)
                                    .or().eq(Message::getToUserId, userId))
                            .orderByDesc(Message::getCreateTime)
                            .last("LIMIT 5"));
            long msgUnread = msgs.stream().filter(m -> m.getIsRead() == 0
                    && m.getToUserId().equals(userId)).count();

            // 2. 通知
            List<SystemMsg> sysMsgs = systemMsgMapper.selectPage(
                    new Page<>(1, 5),
                    new LambdaQueryWrapper<SystemMsg>()
                            .eq(SystemMsg::getUserId, userId)
                            .orderByDesc(SystemMsg::getCreateTime)
            ).getRecords();
            long sysUnread = sysMsgs.stream().filter(m -> m.getIsRead() == 0).count();

            // 3. 公告
            List<Announcement> announcements = announcementMapper.selectList(
                    new LambdaQueryWrapper<Announcement>()
                            .eq(Announcement::getStatus, 1)
                            .orderByDesc(Announcement::getPublishTime)
                            .last("LIMIT 3"));

            // 构建摘要
            Map<Integer, String> typeNames = Map.of(1, "点赞", 2, "评论", 3, "私信", 4, "圈子公告", 5, "系统公告");
            StringBuilder sb = new StringBuilder("📬 消息中心\n");
            sb.append("💬 私信（").append(msgs.size()).append(" 条，未读 ").append(msgUnread).append("）\n");
            for (Message m : msgs) {
                User sender = userMapper.selectById(m.getFromUserId());
                String senderName = sender != null ? sender.getNickname() : ("用户" + m.getFromUserId());
                boolean incoming = m.getToUserId().equals(userId);
                sb.append("  ").append(incoming ? "←" : "→").append(" ").append(senderName)
                        .append(incoming && m.getIsRead() == 0 ? "【未读】" : "")
                        .append("：").append(truncate(m.getContent(), 40)).append("\n");
            }
            sb.append("🔔 通知（").append(sysMsgs.size()).append(" 条，未读 ").append(sysUnread).append("）\n");
            for (SystemMsg m : sysMsgs) {
                String typeName = typeNames.getOrDefault(m.getMsgType(), "其他");
                sb.append("  ").append(m.getIsRead() == 0 ? "【未读】" : "").append("[").append(typeName).append("] ")
                        .append(truncate(m.getContent(), 40)).append("\n");
            }
            sb.append("📢 公告（").append(announcements.size()).append(" 条）\n");
            for (Announcement a : announcements) {
                sb.append("  ").append(a.getTitle()).append("：").append(truncate(a.getContent(), 40)).append("\n");
            }

            // 导航卡片
            List<NavigationAction> nav = new ArrayList<>();
            nav.add(new NavigationAction("消息中心", "/messages", "message_center", "查看所有消息"));
            for (Message m : msgs) {
                boolean incoming = m.getToUserId().equals(userId);
                Long partnerId = incoming ? m.getFromUserId() : m.getToUserId();
                User sender = userMapper.selectById(m.getFromUserId());
                String label = sender != null ? sender.getNickname() : ("用户" + m.getFromUserId());
                nav.add(new NavigationAction("私信: " + label, "/chat/" + partnerId, "chat", label));
            }
            for (SystemMsg m : sysMsgs) {
                if (m.getTargetId() != null) {
                    if (m.getMsgType() == 1 || m.getMsgType() == 2) {
                        nav.add(new NavigationAction("查看帖子", "/post/" + m.getTargetId(), "post", truncate(m.getContent(), 30)));
                    } else if (m.getMsgType() == 4) {
                        nav.add(new NavigationAction("查看圈子", "/circle/" + m.getTargetId(), "circle", truncate(m.getContent(), 30)));
                    }
                }
            }
            for (Announcement a : announcements) {
                nav.add(new NavigationAction(a.getTitle(), "/chat?tab=announce", "announce", truncate(a.getContent(), 40)));
            }

            long totalUnread = msgUnread + sysUnread;
            return ToolResult.builder()
                    .success(true).toolName("get_message_center")
                    .toolDisplayName("消息中心")
                    .data(Map.of("userId", userId, "messages", msgs.size(), "notifications", sysMsgs.size(),
                            "announcements", announcements.size(), "unreadCount", totalUnread))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getMessageCenter error: ", e);
            return ToolResult.failure("get_message_center", "查询消息中心失败");
        }
    }

    private ToolResult searchPostsByTags(String keyword) {
        try {
            if (keyword == null || keyword.isBlank()) {
                return ToolResult.failure("search_posts_by_tags", "请指定标签关键词");
            }
            List<Post> posts = postMapper.selectPage(
                    new Page<>(1, 5),
                    new LambdaQueryWrapper<Post>()
                            .like(Post::getTags, keyword)
                            .eq(Post::getDeleted, 0)
                            .eq(Post::getAuditStatus, 1)
                            .eq(Post::getVisibility, 0)
                            .orderByDesc(Post::getCreateTime)
            ).getRecords();

            if (posts.isEmpty()) {
                return ToolResult.failure("search_posts_by_tags", "没有找到标签包含「" + keyword + "」的帖子");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("🏷️ 标签「").append(keyword).append("」的帖子（共 ").append(posts.size()).append(" 篇）：\n");
            for (int i = 0; i < posts.size(); i++) {
                Post p = posts.get(i);
                sb.append("• ").append(truncate(p.getContent(), 60)).append(postMediaIndicator(p)).append("\n");
                nav.add(new NavigationAction("查看帖子" + (i + 1), "/post/" + p.getId(), "post", truncate(p.getContent(), 30)));
            }

            return ToolResult.builder()
                    .success(true).toolName("search_posts_by_tags")
                    .toolDisplayName("标签搜索结果")
                    .data(Map.of("keyword", keyword, "count", posts.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] searchPostsByTags error: ", e);
            return ToolResult.failure("search_posts_by_tags", "按标签搜索帖子失败");
        }
    }

    private ToolResult searchCirclesByCategory(String keyword) {
        try {
            if (keyword == null || keyword.isBlank()) {
                return ToolResult.failure("search_circles_by_category", "请指定分类关键词");
            }
            List<CircleCategory> categories = circleCategoryMapper.selectList(
                    new LambdaQueryWrapper<CircleCategory>()
                            .like(CircleCategory::getName, keyword)
                            .eq(CircleCategory::getStatus, 1));

            if (categories.isEmpty()) {
                return ToolResult.failure("search_circles_by_category", "没有找到分类「" + keyword + "」");
            }

            StringBuilder sb = new StringBuilder("📂 分类「").append(keyword).append("」下的圈子：\n");
            List<NavigationAction> nav = new ArrayList<>();
            int total = 0;
            for (CircleCategory cat : categories) {
                List<Circle> circles = circleMapper.selectList(
                        new LambdaQueryWrapper<Circle>()
                                .eq(Circle::getCategoryId, cat.getId())
                                .eq(Circle::getDeleted, 0)
                                .eq(Circle::getStatus, 1)
                                .orderByDesc(Circle::getMemberCount)
                                .last("LIMIT 3"));
                for (Circle c : circles) {
                    total++;
                    sb.append("• ").append(c.getName()).append(circleMediaIndicator(c)).append("（").append(c.getMemberCount()).append(" 人）\n");
                    nav.add(new NavigationAction("进入圈子", "/circle/" + c.getId(), "circle", c.getName()));
                }
            }

            if (total == 0) {
                return ToolResult.failure("search_circles_by_category", "分类「" + keyword + "」下暂无圈子");
            }

            sb.insert(0, "共 " + total + " 个圈子：\n");

            return ToolResult.builder()
                    .success(true).toolName("search_circles_by_category")
                    .toolDisplayName("分类搜索")
                    .data(Map.of("keyword", keyword, "count", total))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] searchCirclesByCategory error: ", e);
            return ToolResult.failure("search_circles_by_category", "按分类搜索圈子失败");
        }
    }

    private ToolResult getNewUsers() {
        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            long count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .ge(User::getRegisterTime, sevenDaysAgo));
            List<User> recentUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .ge(User::getRegisterTime, sevenDaysAgo)
                            .orderByDesc(User::getRegisterTime)
                            .last("LIMIT 5"));

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("📅 近 7 天新增 ").append(count).append(" 位用户");
            if (!recentUsers.isEmpty()) {
                sb.append("，最近注册：\n");
                for (int i = 0; i < recentUsers.size(); i++) {
                    User u = recentUsers.get(i);
                    sb.append("• ").append(u.getNickname());
                    if (u.getRegisterTime() != null) {
                        sb.append("（").append(u.getRegisterTime().toLocalDate()).append("）");
                    }
                    sb.append("\n");
                    nav.add(new NavigationAction("查看主页" + (i + 1), "/profile/" + u.getId(), "user", u.getNickname()));
                }
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("newUserCount", count);
            data.put("recentUsers", recentUsers.stream().map(u -> Map.of(
                    "id", u.getId(),
                    "nickname", u.getNickname(),
                    "registerTime", u.getRegisterTime() != null ? u.getRegisterTime().toString() : ""
            )).toList());

            return ToolResult.builder()
                    .success(true).toolName("get_new_users")
                    .toolDisplayName("新用户统计")
                    .data(data).dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getNewUsers error: ", e);
            return ToolResult.failure("get_new_users", "查询新用户失败");
        }
    }

    private ToolResult getCirclePosts(Long circleId) {
        try {
            Circle circle = circleMapper.selectById(circleId);
            if (circle == null) {
                return ToolResult.failure("get_circle_posts", "未找到ID为 " + circleId + " 的圈子");
            }
            List<Post> posts = postMapper.selectPage(
                    new Page<>(1, 5),
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getCircleId, circleId)
                            .eq(Post::getDeleted, 0)
                            .orderByDesc(Post::getCreateTime)
            ).getRecords();

            if (posts.isEmpty()) {
                return ToolResult.failure("get_circle_posts", "圈子「" + circle.getName() + "」暂无帖子");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("圈子「").append(circle.getName()).append("」的最新帖子：\n");
            for (int i = 0; i < posts.size(); i++) {
                Post p = posts.get(i);
                sb.append("• ").append(truncate(p.getContent(), 60)).append(postMediaIndicator(p)).append("\n");
                nav.add(new NavigationAction("查看帖子" + (i + 1), "/post/" + p.getId(), "post", truncate(p.getContent(), 30)));
            }

            return ToolResult.builder()
                    .success(true).toolName("get_circle_posts")
                    .toolDisplayName("圈子帖子")
                    .data(Map.of("circleId", circleId, "circleName", circle.getName(), "count", posts.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getCirclePosts error: ", e);
            return ToolResult.failure("get_circle_posts", "查询圈子帖子失败");
        }
    }

    private ToolResult getHotCircles() {
        try {
            List<Circle> circles = circleMapper.selectList(
                    new LambdaQueryWrapper<Circle>()
                            .eq(Circle::getDeleted, 0)
                            .eq(Circle::getStatus, 1)
                            .orderByDesc(Circle::getMemberCount)
                            .last("LIMIT 5"));

            if (circles.isEmpty()) {
                return ToolResult.failure("get_hot_circles", "暂无热门圈子");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("🔥 热门圈子 Top ").append(circles.size()).append("：\n");
            for (int i = 0; i < circles.size(); i++) {
                Circle c = circles.get(i);
                sb.append("• ").append(c.getName()).append(circleMediaIndicator(c)).append("（").append(c.getMemberCount()).append(" 人）\n");
                nav.add(new NavigationAction("进入圈子", "/circle/" + c.getId(), "circle", c.getName()));
            }

            return ToolResult.builder()
                    .success(true).toolName("get_hot_circles")
                    .toolDisplayName("热门圈子")
                    .data(Map.of("count", circles.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getHotCircles error: ", e);
            return ToolResult.failure("get_hot_circles", "查询热门圈子失败");
        }
    }

    private ToolResult getUserPosts(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return ToolResult.failure("get_user_posts", "未找到ID为 " + userId + " 的用户");
            }
            List<Post> posts = postMapper.selectPage(
                    new Page<>(1, 5),
                    new LambdaQueryWrapper<Post>()
                            .eq(Post::getUserId, userId)
                            .eq(Post::getDeleted, 0)
                            .orderByDesc(Post::getCreateTime)
            ).getRecords();

            if (posts.isEmpty()) {
                return ToolResult.failure("get_user_posts", "用户「" + user.getNickname() + "」暂无帖子");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("用户「").append(user.getNickname()).append("」的最新帖子：\n");
            for (int i = 0; i < posts.size(); i++) {
                Post p = posts.get(i);
                sb.append("• ").append(truncate(p.getContent(), 60)).append(postMediaIndicator(p)).append("\n");
                nav.add(new NavigationAction("查看帖子" + (i + 1), "/post/" + p.getId(), "post", truncate(p.getContent(), 30)));
            }

            return ToolResult.builder()
                    .success(true).toolName("get_user_posts")
                    .toolDisplayName("用户帖子")
                    .data(Map.of("userId", userId, "nickname", user.getNickname(), "count", posts.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getUserPosts error: ", e);
            return ToolResult.failure("get_user_posts", "查询用户帖子失败");
        }
    }

    private ToolResult searchCircles(String keyword) {
        try {
            List<Circle> circles = circleMapper.selectList(
                    new LambdaQueryWrapper<Circle>()
                            .like(Circle::getName, keyword)
                            .eq(Circle::getDeleted, 0)
                            .eq(Circle::getStatus, 1)
                            .orderByDesc(Circle::getMemberCount)
                            .last("LIMIT 5"));

            if (circles.isEmpty()) {
                return ToolResult.failure("search_circles", "没有找到名称包含「" + keyword + "」的圈子");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("找到 ").append(circles.size()).append(" 个相关圈子：\n");
            for (int i = 0; i < circles.size(); i++) {
                Circle c = circles.get(i);
                sb.append("• ").append(c.getName()).append(circleMediaIndicator(c)).append("（").append(c.getMemberCount()).append(" 人）— ").append(truncate(c.getIntro(), 30)).append("\n");
                nav.add(new NavigationAction("进入圈子", "/circle/" + c.getId(), "circle", c.getName()));
            }

            return ToolResult.builder()
                    .success(true).toolName("search_circles")
                    .toolDisplayName("圈子搜索结果")
                    .data(Map.of("keyword", keyword, "count", circles.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] searchCircles error: ", e);
            return ToolResult.failure("search_circles", "搜索圈子失败");
        }
    }

    private ToolResult searchUsers(String keyword) {
        try {
            List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getNickname, keyword)
                            .eq(User::getStatus, 1)
                            .last("LIMIT 5"));

            if (users.isEmpty()) {
                return ToolResult.failure("search_users", "没有找到昵称包含「" + keyword + "」的用户");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("找到 ").append(users.size()).append(" 位相关用户：\n");
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                sb.append("• ").append(u.getNickname());
                if (u.getBio() != null && !u.getBio().isEmpty()) {
                    sb.append("—").append(truncate(u.getBio(), 30));
                }
                sb.append("\n");
                nav.add(new NavigationAction("查看主页", "/profile/" + u.getId(), "user", u.getNickname()));
            }

            return ToolResult.builder()
                    .success(true).toolName("search_users")
                    .toolDisplayName("用户搜索结果")
                    .data(Map.of("keyword", keyword, "count", users.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] searchUsers error: ", e);
            return ToolResult.failure("search_users", "搜索用户失败");
        }
    }

    private ToolResult getComments(Long postId) {
        try {
            Post post = postMapper.selectById(postId);
            if (post == null) {
                return ToolResult.failure("get_comments", "未找到ID为 " + postId + " 的帖子");
            }
            List<Comment> comments = commentMapper.selectList(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getPostId, postId)
                            .orderByDesc(Comment::getCreateTime)
                            .last("LIMIT 5"));

            if (comments.isEmpty()) {
                return ToolResult.failure("get_comments", "该帖子暂无评论");
            }

            List<NavigationAction> nav = new ArrayList<>();
            StringBuilder sb = new StringBuilder("帖子《").append(truncate(post.getContent(), 30)).append("》的最新评论：\n");
            for (int i = 0; i < comments.size(); i++) {
                Comment c = comments.get(i);
                User commenter = userMapper.selectById(c.getUserId());
                String name = commenter != null ? commenter.getNickname() : "用户";
                sb.append("• ").append(name).append("：").append(truncate(c.getContent(), 50)).append("\n");
            }
            nav.add(new NavigationAction("查看帖子", "/post/" + postId, "post", truncate(post.getContent(), 30)));

            return ToolResult.builder()
                    .success(true).toolName("get_comments")
                    .toolDisplayName("评论列表")
                    .data(Map.of("postId", postId, "count", comments.size()))
                    .dataSummary(sb.toString())
                    .navigation(nav).build();
        } catch (Exception e) {
            log.error("[AiTool] getComments error: ", e);
            return ToolResult.failure("get_comments", "查询评论失败");
        }
    }

    private ToolResult getDailyStats() {
        try {
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            long todayPosts = postMapper.selectCount(
                    new LambdaQueryWrapper<Post>().eq(Post::getDeleted, 0).ge(Post::getCreateTime, today));
            long todayComments = commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>().ge(Comment::getCreateTime, today));
            long todayUsers = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().ge(User::getRegisterTime, today));
            long todayCircles = circleMapper.selectCount(
                    new LambdaQueryWrapper<Circle>().eq(Circle::getDeleted, 0).ge(Circle::getCreateTime, today));

            String summary = String.format(
                    "📅 今日数据：\n• 新帖子：%d 篇\n• 新评论：%d 条\n• 新用户：%d 人\n• 新圈子：%d 个",
                    todayPosts, todayComments, todayUsers, todayCircles);

            return ToolResult.builder()
                    .success(true).toolName("get_daily_stats")
                    .toolDisplayName("今日数据")
                    .data(Map.of("todayPosts", todayPosts, "todayComments", todayComments,
                            "todayUsers", todayUsers, "todayCircles", todayCircles))
                    .dataSummary(summary)
                    .navigation(List.of()).build();
        } catch (Exception e) {
            log.error("[AiTool] getDailyStats error: ", e);
            return ToolResult.failure("get_daily_stats", "查询今日数据失败");
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }

    private String postMediaIndicator(Post p) {
        StringBuilder sb = new StringBuilder();
        if (p.getImages() != null && !p.getImages().isEmpty() && !p.getImages().equals("[]")) {
            sb.append(" 📷");
        }
        if (p.getVideo() != null && !p.getVideo().isEmpty()) {
            sb.append(" 📹");
        }
        return sb.toString();
    }

    private String circleMediaIndicator(Circle c) {
        StringBuilder sb = new StringBuilder();
        if (c.getAvatar() != null && !c.getAvatar().isEmpty()) {
            sb.append(" 🖼️");
        }
        if (c.getBanner() != null && !c.getBanner().isEmpty()) {
            sb.append(" 🖼️");
        }
        return sb.toString();
    }

}
