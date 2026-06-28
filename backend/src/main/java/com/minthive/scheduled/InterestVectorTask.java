package com.minthive.scheduled;

import com.minthive.entity.AiUserLog;
import com.minthive.mapper.AiUserLogMapper;
import com.minthive.service.InterestVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterestVectorTask {

    private final AiUserLogMapper aiUserLogMapper;
    private final InterestVectorService interestVectorService;

    /**
     * 每 30 分钟增量更新所有有新增日志的用户的兴趣向量
     */
    @Scheduled(fixedRate = 1800000)
    public void refreshVectors() {
        long start = System.currentTimeMillis();
        // 查找过去 30 分钟内有行为记录的用户
        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        List<AiUserLog> recentLogs = aiUserLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiUserLog>()
                        .ge(AiUserLog::getActionTime, since)
                        .select(AiUserLog::getUserId)
                        .groupBy(AiUserLog::getUserId));
        if (recentLogs.isEmpty()) {
            return;
        }
        List<Long> userIds = recentLogs.stream()
                .map(AiUserLog::getUserId)
                .distinct()
                .collect(Collectors.toList());
        for (Long userId : userIds) {
            try {
                interestVectorService.refreshVector(userId);
            } catch (Exception e) {
                log.error("刷新兴趣向量失败 userId={}", userId, e);
            }
        }
        log.info("兴趣向量批处理完成: {} 个用户, 耗时 {}ms", userIds.size(), System.currentTimeMillis() - start);
    }
}
