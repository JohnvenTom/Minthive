package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.CircleUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 圈子成员 Mapper 接口
 *
 * <p>功能描述：提供 circle_user 表的基础 CRUD 操作</p>
 */
@Mapper
public interface CircleUserMapper extends BaseMapper<CircleUser> {
}
