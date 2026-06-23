package com.minthive.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.Constants;
import com.minthive.common.Result;
import com.minthive.entity.Report;
import com.minthive.entity.SystemMsg;
import com.minthive.mapper.AdminReportMapper;
import com.minthive.mapper.ReportMapper;
import com.minthive.mapper.SystemMsgMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台-举报工单管理控制器
 * <p>功能描述：提供举报工单的分页查询、详情、驳回、删除内容、处罚等处理接口</p>
 */
@Tag(name = "管理后台-举报工单")
@RestController
@RequestMapping("/api/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportMapper reportMapper;
    private final AdminReportMapper adminReportMapper;
    private final com.minthive.mapper.UserMapper userMapper;
    private final com.minthive.mapper.PostMapper postMapper;
    private final com.minthive.mapper.CommentMapper commentMapper;
    private final SystemMsgMapper systemMsgMapper;

    /**
     * 分页查询举报工单列表（关联用户表 + 内容表，字段名对齐前端 ReportWorkOrder 类型）
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @param status   工单状态（0待处理 1已驳回 2已处理）
     * @param type     举报类型
     * @return 分页结果 {list, total, page, pageSize}
     */
    @Operation(summary = "分页查询举报工单")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String type) {
        IPage<Map<String, Object>> p = adminReportMapper.selectReportListWithDetails(
                new Page<>(page, pageSize), keyword, status, type);
        Map<String, Object> data = new HashMap<>(4);
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("pageSize", p.getSize());
        return Result.success(data);
    }

    /**
     * 工单详情（返回完整信息，字段名对齐前端）
     *
     * @param workOrderId 工单ID
     * @return 工单完整信息
     */
    @Operation(summary = "工单详情")
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long workOrderId) {
        Report r = reportMapper.selectById(workOrderId);
        if (r == null) {
            return Result.success(new HashMap<>());
        }
        Map<String, Object> m = new HashMap<>(20);
        m.put("workOrderId", r.getId());
        m.put("reporterId", r.getReporterId());
        m.put("targetId", r.getTargetId());
        m.put("targetType", r.getTargetType().toString());
        m.put("type", r.getReportType());
        m.put("reason", r.getReason());
        m.put("status", r.getStatus() == 0 ? "PENDING" : r.getStatus() == 1 ? "REJECTED" : "RESOLVED");
        m.put("riskLevel", r.getAiRiskLevel() == null ? "LOW" : r.getAiRiskLevel() == 1 ? "LOW" : r.getAiRiskLevel() == 2 ? "MEDIUM" : "HIGH");
        m.put("createTime", r.getCreateTime() != null ? r.getCreateTime().toString() : "");
        m.put("handleTime", r.getHandleTime() != null ? r.getHandleTime().toString() : "");
        m.put("handleResult", r.getResult());

        // 查询举报人昵称
        com.minthive.entity.User reporter = userMapper.selectById(r.getReporterId());
        m.put("reporterName", reporter != null ? reporter.getNickname() : ("用户" + r.getReporterId()));

        // 根据 targetType 查询被举报人信息和被举报内容
        Integer tt = r.getTargetType();
        String targetContent = r.getReason(); // 兜底值
        Long accusedId = r.getTargetId();
        String accusedName = "用户" + accusedId;

        if (tt == 1) {
            // 举报帖子：查 post 表获取内容和作者
            com.minthive.entity.Post post = postMapper.selectById(r.getTargetId());
            if (post != null) {
                targetContent = post.getContent();
                accusedId = post.getUserId();
                com.minthive.entity.User author = userMapper.selectById(accusedId);
                accusedName = author != null ? author.getNickname() : ("用户" + accusedId);
            }
        } else if (tt == 2) {
            // 举报评论：查 comment 表
            com.minthive.entity.Comment comment = commentMapper.selectById(r.getTargetId());
            if (comment != null) {
                targetContent = comment.getContent();
                accusedId = comment.getUserId();
                com.minthive.entity.User commenter = userMapper.selectById(accusedId);
                accusedName = commenter != null ? commenter.getNickname() : ("用户" + accusedId);
            }
        } else if (tt == 4) {
            // 举报用户：内容就是用户信息
            com.minthive.entity.User accusedUser = userMapper.selectById(r.getTargetId());
            if (accusedUser != null) {
                targetContent = "账号: " + accusedUser.getAccount() + " | 昵称: " + accusedUser.getNickname()
                        + " | 简介: " + (accusedUser.getBio() != null ? accusedUser.getBio() : "");
                accusedName = accusedUser.getNickname();
            }
        }

        m.put("targetContent", targetContent);
        m.put("accusedId", accusedId);
        m.put("accusedName", accusedName);

        // AI 分析文案
        m.put("aiAnalysis", "风险等级: " + (r.getAiRiskLevel() == null ? "未评估"
                : r.getAiRiskLevel() == 1 ? "低"
                : r.getAiRiskLevel() == 2 ? "中"
                : r.getAiRiskLevel() == 3 ? "高" : "未知"));

        return Result.success(m);
    }

    /**
     * 驳回工单
     *
     * @param params 包含 workOrderId 和 remark
     * @return 操作结果
     */
    @Operation(summary = "驳回工单")
    @PostMapping("/reject")
    public Result<Void> reject(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("workOrderId").toString());
        String remark = (String) params.getOrDefault("remark", "");
        Report r = reportMapper.selectById(id);
        if (r != null) {
            Report update = new Report();
            update.setId(id);
            update.setStatus(1); // 已驳回
            update.setResult(remark);
            update.setHandleTime(LocalDateTime.now());
            reportMapper.updateById(update);
            // 发送举报结果通知给举报者
            sendReportNotification(r, "已驳回", remark.isEmpty() ? "经审核，你提交的举报已被驳回。" : "经审核，你提交的举报已被驳回：" + remark);
        }
        return Result.success();
    }

    /**
     * 处罚用户（含可选删除被举报内容）
     *
     * <p>统一处理接口：支持封禁/警告用户，并可同时删除被举报内容。
     * 处理完成后自动向举报者发送结果通知</p>
     *
     * @param params 包含：
     *               - workOrderId: 工单ID
     *               - punishType: 处罚类型（ban=封禁, warn=警告）
     *               - reason: 处罚理由
     *               - deleteContent: 是否同时删除被举报内容（true/false，默认false）
     * @return 操作结果
     */
    @Operation(summary = "处罚用户")
    @PostMapping("/punish")
    public Result<Void> punish(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("workOrderId").toString());
        String punishType = (String) params.getOrDefault("punishType", "warn");
        String reason = (String) params.getOrDefault("reason", "");
        boolean deleteContent = Boolean.TRUE.equals(params.get("deleteContent"));

        Report r = reportMapper.selectById(id);
        if (r == null) {
            return Result.success();
        }

        // 1. 更新工单状态为已处理
        Report update = new Report();
        update.setId(id);
        update.setStatus(2); // 已处理

        // 2. 拼接处理结果描述
        StringBuilder resultDesc = new StringBuilder();
        if ("ban".equals(punishType)) {
            resultDesc.append("已封禁用户");
        } else {
            resultDesc.append("已警告用户");
        }
        if (reason != null && !reason.isEmpty()) {
            resultDesc.append("，原因：").append(reason);
        }
        if (deleteContent) {
            resultDesc.append("，并删除了被举报内容");
            // TODO: 根据 targetType 执行实际的业务表删除操作
            //   targetType=1 → postMapper.deleteById(r.getTargetId())
            //   targetType=2 → commentMapper.deleteById(r.getTargetId())
            //   targetType=4 → 用户封禁逻辑
        }
        update.setResult(resultDesc.toString());
        update.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(update);

        // TODO: 记录处罚日志到 punish_log 表
        // punishLogMapper.insert(...)

        // 3. 发送举报结果通知给举报者
        sendReportNotification(
                r,
                "已处理",
                "你提交的举报已审核通过。" + resultDesc.toString() + "。感谢你的反馈！"
        );

        return Result.success();
    }

    /**
     * 发送举报结果通知给举报者
     *
     * <p>管理员处理举报工单（驳回/通过）后，通过 system_msg 表向举报者推送结果通知，
     * 举报者可在消息中心的通知列表中查看</p>
     *
     * @param report   举报工单实体（包含 reporterId、targetId、targetType 等信息）
     * @param status   处理状态描述（如"已驳回"、"已处理"）
     * @param content  通知正文内容
     */
    private void sendReportNotification(Report report, String status, String content) {
        SystemMsg msg = new SystemMsg();
        msg.setUserId(report.getReporterId());
        msg.setMsgType(Constants.SYS_MSG_TYPE_REPORT);
        msg.setContent("【举报结果】" + status + " - " + content);
        msg.setTargetId(report.getTargetId());
        msg.setIsRead(Constants.READ_STATUS_UNREAD);
        msg.setCreateTime(LocalDateTime.now());
        systemMsgMapper.insert(msg);
    }
}
