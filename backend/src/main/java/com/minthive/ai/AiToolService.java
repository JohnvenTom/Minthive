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

    public ToolResult execute(IntentParser.ParsedIntent intent) {
        return switch (intent.getType()) {
            case GET_POST -> getPostDetail((Long) intent.getParams().get("id"));
            case GET_CIRCLE -> getCircleDetail((Long) intent.getParams().get("id"));
            case GET_USER -> getUserDetail((Long) intent.getParams().get("id"));
            case GET_STATS -> getPlatformStats();
            case SEARCH_POSTS -> searchPosts((String) intent.getParams().get("keyword"));
            case GET_TRENDING -> getTrendingPosts();
            case GET_NEW_USERS -> getNewUsers();
            default -> null;
        };
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

            String summary = String.format("找到帖子《%s》——作者：%s，点赞 %d，评论 %d",
                    truncate(post.getContent(), 50),
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

            String summary = String.format("圈子「%s」—— %s，成员 %d 人，帖子 %d 篇",
                    circle.getName(),
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
                sb.append("• ").append(truncate(p.getContent(), 60)).append("\n");
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
                sb.append("• ").append(truncate(p.getContent(), 60)).append("\n");
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

            StringBuilder sb = new StringBuilder("📅 近 7 天新增 ").append(count).append(" 位用户");
            if (!recentUsers.isEmpty()) {
                sb.append("，最近注册：\n");
                for (User u : recentUsers) {
                    sb.append("• ").append(u.getNickname());
                    if (u.getRegisterTime() != null) {
                        sb.append("（").append(u.getRegisterTime().toLocalDate()).append("）");
                    }
                    sb.append("\n");
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
                    .navigation(List.of()).build();
        } catch (Exception e) {
            log.error("[AiTool] getNewUsers error: ", e);
            return ToolResult.failure("get_new_users", "查询新用户失败");
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
