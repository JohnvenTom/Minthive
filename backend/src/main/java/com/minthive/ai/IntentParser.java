package com.minthive.ai;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class IntentParser {

    public ParsedIntent parse(String question) {
        if (question == null || question.isBlank()) {
            return new ParsedIntent(IntentType.NONE, null);
        }
        String q = question.trim().toLowerCase();

        // 圈子帖子: "圈子 5 的帖子", "circle 3 posts"（放在 get_circle 前面，避免冲突）
        Matcher circlePostMatcher = Pattern.compile("(?:圈子|circle)\\s*#?\\s*(\\d+)\\s*(?:的|of|'s)?\\s*(?:帖子|post|post)").matcher(q);
        if (circlePostMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("circleId", Long.parseLong(circlePostMatcher.group(1)));
            return new ParsedIntent(IntentType.GET_CIRCLE_POSTS, params);
        }

        // 圈子详情: "圈子 5", "circle 3"
        Matcher circleMatcher = Pattern.compile("(?:圈子|circle)\\s*#?\\s*(\\d+)").matcher(q);
        if (circleMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", Long.parseLong(circleMatcher.group(1)));
            return new ParsedIntent(IntentType.GET_CIRCLE, params);
        }

        // 用户帖子: "用户 7 的帖子", "user 3 posts"（放在 get_user 前面，避免冲突）
        Matcher userPostMatcher = Pattern.compile("(?:用户|user)\\s*#?\\s*(\\d+)\\s*(?:的|of|'s)?\\s*(?:帖子|post|post)").matcher(q);
        if (userPostMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("userId", Long.parseLong(userPostMatcher.group(1)));
            return new ParsedIntent(IntentType.GET_USER_POSTS, params);
        }

        // 用户详情: "用户 42", "user 7"
        Matcher userMatcher = Pattern.compile("(?:用户|user)\\s*#?\\s*(\\d+)").matcher(q);
        if (userMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", Long.parseLong(userMatcher.group(1)));
            return new ParsedIntent(IntentType.GET_USER, params);
        }

        // 帖子评论: "帖子 42 的评论", "post 5 comments"（放在 get_post 前面，避免冲突）
        Matcher commentMatcher = Pattern.compile("(?:帖子|post)\\s*#?\\s*(\\d+)\\s*(?:的|of|'s)?\\s*(?:评论|comment|回复)").matcher(q);
        if (commentMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("postId", Long.parseLong(commentMatcher.group(1)));
            return new ParsedIntent(IntentType.GET_COMMENTS, params);
        }

        // 帖子详情: "帖子 123", "post 42", "查看帖子 5"
        Matcher postMatcher = Pattern.compile("(?:帖子|post)\\s*#?\\s*(\\d+)").matcher(q);
        if (postMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", Long.parseLong(postMatcher.group(1)));
            return new ParsedIntent(IntentType.GET_POST, params);
        }

        // 特定搜索（先于通用搜索匹配，避免"搜索圈子 编程"被通用搜索吞掉）
        // 搜索圈子: "搜索圈子 编程", "搜一下圈子 旅行"
        Matcher searchCircleMatcher = Pattern.compile("(?:搜索|搜一下|找一下|查找|search|find)\\s*(?:圈子|circle)\\s+(.+)").matcher(q);
        if (searchCircleMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", searchCircleMatcher.group(1).trim());
            return new ParsedIntent(IntentType.SEARCH_CIRCLES, params);
        }
        // 搜索用户: "搜索用户 小明", "搜一下用户 小王"
        Matcher searchUserMatcher = Pattern.compile("(?:搜索|搜一下|找一下|查找|search|find)\\s*(?:用户|user)\\s+(.+)").matcher(q);
        if (searchUserMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", searchUserMatcher.group(1).trim());
            return new ParsedIntent(IntentType.SEARCH_USERS, params);
        }
        // 通用搜索帖子: "搜索 编程", "找一下 旅行"（放在特定搜索后面）
        Matcher searchMatcher = Pattern.compile("(?:搜索|搜一下|找一下|查找|search|find)\\s+(.+)").matcher(q);
        if (searchMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", searchMatcher.group(1).trim());
            return new ParsedIntent(IntentType.SEARCH_POSTS, params);
        }

        // 平台统计: "统计", "有多少用户", "总帖子数", "平台数据"
        if (containsAny(q, "统计", "有多少", "总用户", "总帖子", "平台数据", "概况",
                "stats", "statistics", "how many", "total")) {
            return new ParsedIntent(IntentType.GET_STATS, null);
        }

        // 热门帖子: "热门", "热门帖子", "trending"
        if (containsAny(q, "热门", "trending", "hot", "最热", "流行")) {
            return new ParsedIntent(IntentType.GET_TRENDING, null);
        }

        // 热门圈子: "热门圈子", "hot circles", "推荐圈子"
        if (containsAny(q, "热门圈子", "hot circle", "推荐圈子", "有哪些圈子", "所有圈子", "圈子推荐")) {
            return new ParsedIntent(IntentType.GET_HOT_CIRCLES, null);
        }

        // 新用户: "新用户", "new users", "最近注册"
        if (containsAny(q, "新用户", "new user", "最近注册", "新增用户")) {
            return new ParsedIntent(IntentType.GET_NEW_USERS, null);
        }

        // 今日数据: "今日数据", "daily stats", "今天统计"
        if (containsAny(q, "今日数据", "今日统计", "今天数据", "daily stats", "今天")) {
            return new ParsedIntent(IntentType.GET_DAILY_STATS, null);
        }

        return new ParsedIntent(IntentType.NONE, null);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw.toLowerCase())) return true;
        }
        return false;
    }

    public enum IntentType {
        NONE, GET_POST, GET_CIRCLE, GET_USER, GET_STATS, SEARCH_POSTS, GET_TRENDING, GET_NEW_USERS,
        GET_CIRCLE_POSTS, GET_HOT_CIRCLES, GET_USER_POSTS, SEARCH_CIRCLES, SEARCH_USERS, GET_COMMENTS, GET_DAILY_STATS
    }

    @Data
    public static class ParsedIntent {
        private final IntentType type;
        private final Map<String, Object> params;
    }
}
