package com.minthive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minthive.common.RedisConstants;
import com.minthive.common.TagMappingConstants;
import com.minthive.entity.AiUserLog;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.mapper.AiUserLogMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.util.RedisUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterestVectorService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiUserLogMapper aiUserLogMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final TagMappingService tagMappingService;

    public Map<String, Double> computeVector(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<AiUserLog> wrapper = new LambdaQueryWrapper<AiUserLog>()
                .eq(AiUserLog::getUserId, userId)
                .ge(AiUserLog::getActionTime, since);
        List<AiUserLog> logs = aiUserLogMapper.selectList(wrapper);

        if (logs.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> postIds = logs.stream()
                .map(AiUserLog::getPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Post> postMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            postMapper.selectBatchIds(postIds).forEach(p -> postMap.put(p.getId(), p));
        }

        Map<String, Double> rawScores = new HashMap<>();

        for (AiUserLog log : logs) {
            Post post = postMap.get(log.getPostId());
            if (post == null) continue;

            Set<String> tags;
            if (log.getInterestSnapshot() != null && !log.getInterestSnapshot().isBlank()) {
                tags = TagMappingConstants.matchTagsFromString(log.getInterestSnapshot());
            } else {
                tags = tagMappingService.resolvePostTags(post);
            }
            if (tags.isEmpty()) continue;

            double weight = getActionWeight(log.getActionType(), log.getDuration());
            double score = weight / Math.max(tags.size(), 1);

            for (String tag : tags) {
                rawScores.merge(tag, score, Double::sum);
            }
        }

        if (rawScores.isEmpty()) {
            return Collections.emptyMap();
        }

        double max = rawScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double min = rawScores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double range = Math.max(max - min, 0.01);

        Map<String, Double> normalized = new LinkedHashMap<>();
        rawScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> normalized.put(e.getKey(), (e.getValue() - min) / range));

        return normalized;
    }

    public Map<String, Double> refreshVector(Long userId) {
        Map<String, Double> vector = computeVector(userId);
        String json = vectorToJson(vector);
        User update = new User();
        update.setId(userId);
        update.setAiInterestVector(json);
        userMapper.updateById(update);
        String key = RedisConstants.AI_INTEREST_VECTOR_PREFIX + userId;
        redisUtil.set(key, json, 1, TimeUnit.HOURS);
        return vector;
    }

    public Map<String, Double> getVector(Long userId) {
        String key = RedisConstants.AI_INTEREST_VECTOR_PREFIX + userId;
        Object cached = redisUtil.get(key);
        if (cached instanceof String) {
            return jsonToMap((String) cached);
        }
        User user = userMapper.selectById(userId);
        if (user != null && user.getAiInterestVector() != null) {
            Map<String, Double> vector = jsonToMap(user.getAiInterestVector());
            if (!vector.isEmpty()) {
                redisUtil.set(key, user.getAiInterestVector(), 1, TimeUnit.HOURS);
            }
            return vector;
        }
        // 兼容：已有 interestTags 但尚未生成 aiInterestVector 的存量用户
        if (user != null && user.getInterestTags() != null && !user.getInterestTags().isBlank()) {
            Map<String, Double> fallback = new LinkedHashMap<>();
            for (String tag : user.getInterestTags().split(",")) {
                tag = tag.trim();
                if (!tag.isEmpty()) {
                    fallback.put(tag, 1.0);
                }
            }
            if (!fallback.isEmpty()) {
                String json = vectorToJson(fallback);
                User update = new User();
                update.setId(userId);
                update.setAiInterestVector(json);
                userMapper.updateById(update);
                redisUtil.set(key, json, 1, TimeUnit.HOURS);
                return fallback;
            }
        }
        return Collections.emptyMap();
    }

    private double getActionWeight(String actionType, Integer duration) {
        if (actionType == null) return 0;
        return switch (actionType) {
            case "view" -> 1;
            case "like" -> 3;
            case "comment" -> 5;
            case "collect" -> 5;
            case "unlike", "uncollect" -> -1;
            case "stay" -> duration != null ? duration / 60.0 : 0.5;
            default -> 0;
        };
    }

    private String vectorToJson(Map<String, Double> vector) {
        try {
            return OBJECT_MAPPER.writeValueAsString(vector);
        } catch (Exception e) {
            log.error("序列化兴趣向量失败", e);
            return "{}";
        }
    }

    private Map<String, Double> jsonToMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
