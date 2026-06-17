package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Report;
import com.minthive.mapper.ReportMapper;
import com.minthive.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 举报工单服务实现
 */
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    /**
     * 提交举报
     *
     * @param report 举报实体
     * @return 举报实体
     */
    @Override
    public Report submit(Report report) {
        report.setStatus(0);
        report.setAiRiskLevel(0);
        reportMapper.insert(report);
        return report;
    }

    /**
     * 分页查询举报工单
     *
     * @param current 当前页
     * @param size    每页大小
     * @param status  状态
     * @return 分页结果
     */
    @Override
    public Page<Report> page(long current, long size, Integer status) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(status != null, Report::getStatus, status)
                .orderByDesc(Report::getAiRiskLevel)
                .orderByDesc(Report::getCreateTime);
        return reportMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 处理工单
     *
     * @param id     工单ID
     * @param status 处理状态
     * @param result 处理结果
     */
    @Override
    public void handle(Long id, Integer status, String result) {
        Report update = new Report();
        update.setId(id);
        update.setStatus(status);
        update.setResult(result);
        update.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(update);
    }

    /**
     * AI 风险等级评估
     *
     * @param id    工单ID
     * @param level 风险等级
     */
    @Override
    public void assessRisk(Long id, Integer level) {
        Report update = new Report();
        update.setId(id);
        update.setAiRiskLevel(level);
        reportMapper.updateById(update);
    }
}
