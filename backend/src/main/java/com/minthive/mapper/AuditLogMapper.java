package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计主表 Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
