package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.AiUserLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI用户行为日志 Mapper 接口
 *
 * <p>功能描述：提供 ai_user_log 表的基础 CRUD 操作</p>
 */
@Mapper
public interface AiUserLogMapper extends BaseMapper<AiUserLog> {
}
