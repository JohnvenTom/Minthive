package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 *
 * <p>功能描述：提供 user 表的基础 CRUD 操作</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
