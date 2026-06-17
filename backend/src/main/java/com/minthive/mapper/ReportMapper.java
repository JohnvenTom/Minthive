package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minthive.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报工单 Mapper 接口
 *
 * <p>功能描述：提供 report 表的基础 CRUD 操作</p>
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
