package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 私信消息 Mapper 接口
 *
 * <p>功能描述：提供 message 表的基础 CRUD 操作</p>
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
