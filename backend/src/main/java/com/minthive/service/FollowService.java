package com.minthive.service;

import com.minthive.entity.Follow;

/**
 * 关注服务接口
 */
public interface FollowService {

    /**
     * 关注用户
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     */
    void follow(Long userId, Long followUserId);

    /**
     * 取消关注
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     */
    void unfollow(Long userId, Long followUserId);

    /**
     * 查询是否已关注
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     * @return true 已关注
     */
    boolean isFollowing(Long userId, Long followUserId);
}
