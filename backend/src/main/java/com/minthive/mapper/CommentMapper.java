package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper 接口
 *
 * <p>功能描述：提供 comment 表的基础 CRUD 操作</p>
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
