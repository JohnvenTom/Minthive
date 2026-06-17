package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.Post;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动态帖子 Mapper 接口
 *
 * <p>功能描述：提供 post 表的基础 CRUD 操作</p>
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
