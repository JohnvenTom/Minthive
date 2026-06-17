package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.Follow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户关注 Mapper 接口
 *
 * <p>功能描述：提供 follow 表的基础 CRUD 操作</p>
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
}
