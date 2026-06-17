package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.SystemMsg;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统消息 Mapper 接口
 *
 * <p>功能描述：提供 system_msg 表的基础 CRUD 操作</p>
 */
@Mapper
public interface SystemMsgMapper extends BaseMapper<SystemMsg> {
}
