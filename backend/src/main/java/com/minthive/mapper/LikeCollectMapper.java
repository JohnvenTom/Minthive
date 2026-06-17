package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.LikeCollect;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞收藏 Mapper 接口
 *
 * <p>功能描述：提供 like_collect 表的基础 CRUD 操作</p>
 */
@Mapper
public interface LikeCollectMapper extends BaseMapper<LikeCollect> {
}
