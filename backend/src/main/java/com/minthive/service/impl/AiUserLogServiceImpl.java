package com.minthive.service.impl;

import com.minthive.entity.AiUserLog;
import com.minthive.entity.User;
import com.minthive.mapper.AiUserLogMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.service.AiUserLogService;
import com.minthive.service.InterestVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * AI用户行为日志服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AiUserLogServiceImpl implements AiUserLogService {

    private final AiUserLogMapper aiUserLogMapper;

    private final UserMapper userMapper;

    private final InterestVectorService interestVectorService;

    /**
     * 记录用户行为日志
     *
     * @param log 日志实体
     */
    @Async
    @Override
    public void record(AiUserLog log) {
        aiUserLogMapper.insert(log);
        // 行为发生后立即刷新兴趣向量，保证推荐近实时
        interestVectorService.refreshVector(log.getUserId());
    }

    /**
     * 更新用户兴趣向量
     *
     * @param userId 用户ID
     * @param vector 兴趣向量(JSON)
     */
    @Override
    public void updateInterestVector(Long userId, String vector) {
        User update = new User();
        update.setId(userId);
        update.setAiInterestVector(vector);
        userMapper.updateById(update);
    }
}
