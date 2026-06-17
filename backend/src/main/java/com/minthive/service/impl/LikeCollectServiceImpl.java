package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minthive.entity.LikeCollect;
import com.minthive.mapper.LikeCollectMapper;
import com.minthive.service.LikeCollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 点赞收藏服务实现
 */
@Service
public class LikeCollectServiceImpl implements LikeCollectService {

    @Autowired
    private LikeCollectMapper likeCollectMapper;

    /**
     * 点赞/收藏(幂等)
     *
     * @param userId   用户ID
     * @param targetId 目标ID
     * @param type     类型
     * @return true 新增 / false 已存在
     */
    @Override
    public boolean like(Long userId, Long targetId, Integer type) {
        Long exist = likeCollectMapper.selectCount(new LambdaQueryWrapper<LikeCollect>()
                .eq(LikeCollect::getUserId, userId)
                .eq(LikeCollect::getTargetId, targetId)
                .eq(LikeCollect::getType, type));
        if (exist > 0) {
            return false;
        }
        LikeCollect lc = new LikeCollect();
        lc.setUserId(userId);
        lc.setTargetId(targetId);
        lc.setType(type);
        likeCollectMapper.insert(lc);
        return true;
    }

    /**
     * 取消点赞/收藏
     *
     * @param userId   用户ID
     * @param targetId 目标ID
     * @param type     类型
     */
    @Override
    public void unlike(Long userId, Long targetId, Integer type) {
        likeCollectMapper.delete(new LambdaQueryWrapper<LikeCollect>()
                .eq(LikeCollect::getUserId, userId)
                .eq(LikeCollect::getTargetId, targetId)
                .eq(LikeCollect::getType, type));
    }

    /**
     * 查询是否已点赞/收藏
     *
     * @param userId   用户ID
     * @param targetId 目标ID
     * @param type     类型
     * @return true 已操作
     */
    @Override
    public boolean isLiked(Long userId, Long targetId, Integer type) {
        Long count = likeCollectMapper.selectCount(new LambdaQueryWrapper<LikeCollect>()
                .eq(LikeCollect::getUserId, userId)
                .eq(LikeCollect::getTargetId, targetId)
                .eq(LikeCollect::getType, type));
        return count > 0;
    }
}
