package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.Circle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 兴趣圈子 Mapper 接口
 *
 * <p>功能描述：提供 circle 表的基础 CRUD 操作</p>
 */
@Mapper
public interface CircleMapper extends BaseMapper<Circle> {
}
