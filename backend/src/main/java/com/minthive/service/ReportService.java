package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Report;

/**
 * 举报工单服务接口
 */
public interface ReportService {

    /**
     * 提交举报
     *
     * @param report 举报实体
     * @return 举报实体
     */
    Report submit(Report report);

    /**
     * 分页查询举报工单
     *
     * @param current 当前页
     * @param size    每页大小
     * @param status  状态(可空)
     * @return 分页结果
     */
    Page<Report> page(long current, long size, Integer status);

    /**
     * 处理工单
     *
     * @param id     工单ID
     * @param status 处理状态
     * @param result 处理结果
     */
    void handle(Long id, Integer status, String result);

    /**
     * AI 风险等级评估
     *
     * @param id     工单ID
     * @param level  风险等级
     */
    void assessRisk(Long id, Integer level);
}
