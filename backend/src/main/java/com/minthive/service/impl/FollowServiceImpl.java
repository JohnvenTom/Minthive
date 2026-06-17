package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.entity.Follow;
import com.minthive.mapper.FollowMapper;
import com.minthive.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 关注服务实现
 */
@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private FollowMapper followMapper;

    /**
     * 关注用户
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     * @throws BusinessException 重复关注时抛出
     */
    @Override
    public void follow(Long userId, Long followUserId) {
        if (userId.equals(followUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能关注自己");
        }
        Long exist = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId));
        if (exist > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_OPERATION, "已关注该用户");
        }
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        followMapper.insert(follow);
    }

    /**
     * 取消关注
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     */
    @Override
    public void unfollow(Long userId, Long followUserId) {
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId));
    }

    /**
     * 查询是否已关注
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     * @return true 已关注
     */
    @Override
    public boolean isFollowing(Long userId, Long followUserId) {
        Long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId));
        return count > 0;
    }
}
