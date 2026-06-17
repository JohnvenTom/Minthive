package com.minthive.service;

import com.minthive.entity.AiUserLog;

/**
 * AI用户行为日志服务接口
 */
public interface AiUserLogService {

    /**
     * 记录用户行为日志
     *
     * @param log 日志实体
     */
    void record(AiUserLog log);

    /**
     * 更新用户兴趣向量
     *
     * @param userId  用户ID
     * @param vector  兴趣向量(JSON)
     */
    void updateInterestVector(Long userId, String vector);
}
