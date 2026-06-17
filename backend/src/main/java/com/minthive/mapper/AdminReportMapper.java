package com.minthive.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 管理后台-举报工单 Mapper
 * <p>提供关联查询，补全举报人昵称、被举报人昵称、被举报内容等字段</p>
 */
@Mapper
public interface AdminReportMapper {

    /**
     * 分页查询举报工单（关联用户表 + 内容表）
     * <p>返回字段与前端 ReportWorkOrder 类型完全对齐</p>
     *
     * @param page     分页参数
     * @param keyword  搜索关键词（匹配举报原因）
     * @param status   工单状态（0待处理 1已驳回 2已处理）
     * @param type     举报类型
     * @return 分页结果（每条记录为 Map，字段名对齐前端）
     */
    IPage<Map<String, Object>> selectReportListWithDetails(
            Page<Map<String, Object>> page,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("type") String type);
}
