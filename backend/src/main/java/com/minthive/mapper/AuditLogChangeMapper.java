package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.AuditLogChange;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计变更子表 Mapper
 */
@Mapper
public interface AuditLogChangeMapper extends BaseMapper<AuditLogChange> {
}
