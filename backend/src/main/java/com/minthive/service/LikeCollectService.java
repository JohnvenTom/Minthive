package com.minthive.service;

import com.minthive.entity.LikeCollect;

/**
 * 点赞收藏服务接口
 */
public interface LikeCollectService {

    /**
     * 点赞/收藏(幂等)
     *
     * @param userId   用户ID
     * @param targetId 目标ID
     * @param type     类型
     * @return true 新增 / false 已存在
     */
    boolean like(Long userId, Long targetId, Integer type);

    /**
     * 取消点赞/收藏
     *
     * @param userId   用户ID
     * @param targetId 目标ID
     * @param type     类型
     */
    void unlike(Long userId, Long targetId, Integer type);

    /**
     * 查询是否已点赞/收藏
     *
     * @param userId   用户ID
     * @param targetId 目标ID
     * @param type     类型
     * @return true 已操作
     */
    boolean isLiked(Long userId, Long targetId, Integer type);
}
