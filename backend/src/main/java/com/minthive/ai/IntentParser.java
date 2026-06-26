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

        // 帖子详情: "帖子 123", "post 42", "查看帖子 5"
        Matcher postMatcher = Pattern.compile("(?:帖子|post)\\s*#?\\s*(\\d+)").matcher(q);
        if (postMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", Long.parseLong(postMatcher.group(1)));
            return new ParsedIntent(IntentType.GET_POST, params);
        }

        // 圈子详情: "圈子 5", "circle 3"
        Matcher circleMatcher = Pattern.compile("(?:圈子|circle)\\s*#?\\s*(\\d+)").matcher(q);
        if (circleMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", Long.parseLong(circleMatcher.group(1)));
            return new ParsedIntent(IntentType.GET_CIRCLE, params);
        }

        // 用户详情: "用户 42", "user 7"
        Matcher userMatcher = Pattern.compile("(?:用户|user)\\s*#?\\s*(\\d+)").matcher(q);
        if (userMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", Long.parseLong(userMatcher.group(1)));
            return new ParsedIntent(IntentType.GET_USER, params);
        }

        // 平台统计: "统计", "有多少用户", "总帖子数", "平台数据"
        if (containsAny(q, "统计", "有多少", "总用户", "总帖子", "平台数据", "概况",
                "stats", "statistics", "how many", "total")) {
            return new ParsedIntent(IntentType.GET_STATS, null);
        }

        // 搜索帖子: "搜索 编程", "找一下 旅行"
        Matcher searchMatcher = Pattern.compile("(?:搜索|搜一下|找一下|查找|search|find)\\s+(.+)").matcher(q);
        if (searchMatcher.find()) {
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", searchMatcher.group(1).trim());
            return new ParsedIntent(IntentType.SEARCH_POSTS, params);
        }

        // 热门帖子: "热门", "热门帖子", "trending"
        if (containsAny(q, "热门", "trending", "hot", "最热", "流行")) {
            return new ParsedIntent(IntentType.GET_TRENDING, null);
        }

        // 新用户: "新用户", "new users", "最近注册"
        if (containsAny(q, "新用户", "new user", "最近注册", "新增用户")) {
            return new ParsedIntent(IntentType.GET_NEW_USERS, null);
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
        NONE, GET_POST, GET_CIRCLE, GET_USER, GET_STATS, SEARCH_POSTS, GET_TRENDING, GET_NEW_USERS
    }

    @Data
    public static class ParsedIntent {
        private final IntentType type;
        private final Map<String, Object> params;
    }
}
