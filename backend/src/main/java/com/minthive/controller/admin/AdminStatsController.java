package com.minthive.controller.admin;

import com.minthive.ai.AiContext;
import com.minthive.common.Result;
import com.minthive.config.AiConfig;
import com.minthive.mapper.AdminStatsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 管理后台-数据统计控制器
 * <p>功能描述：提供数据大屏所需的全部统计接口，包括核心指标、趋势图、排行、AI 日报等</p>
 * <p>注意事项：所有日期范围参数支持 DAY(近30天)/WEEK(近12周)/MONTH(近12月) 三种维度</p>
 */
@Tag(name = "管理后台-数据统计")
@RestController
@RequestMapping("/api/admin/stats")
@Slf4j
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsMapper adminStatsMapper;
    private final AiContext aiContext;
    private final AiConfig aiConfig;

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 核心指标总览
     * <p>功能：返回平台当前的核心运营数据，包括用户总数、今日注册、日活/月活、帖子数、互动量、待处理工单，以及较昨日趋势百分比</p>
     *
     * @return 包含指标字段及 xxxTrend 趋势字段的 Map
     */
    @Operation(summary = "核心指标")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        Map<String, Object> data = adminStatsMapper.selectCoreMetrics();

        // 确保数值字段为 Number
        long totalUsers = toNumber(data.get("totalUsers")).longValue();
        long todayRegister = toNumber(data.get("todayRegister")).longValue();
        long dau = toNumber(data.get("dau")).longValue();
        long mau = toNumber(data.get("mau")).longValue();
        long totalPosts = toNumber(data.get("totalPosts")).longValue();
        long todayPosts = toNumber(data.get("todayPosts")).longValue();
        long totalInteractions = toNumber(data.get("totalInteractions")).longValue();
        long pendingReports = toNumber(data.get("pendingReports")).longValue();

        // 昨日对比值
        long prevTotalUsers = toNumber(data.get("prevTotalUsers")).longValue();
        long prevTodayRegister = toNumber(data.get("prevTodayRegister")).longValue();
        long prevDau = toNumber(data.get("prevDau")).longValue();
        long prevMau = toNumber(data.get("prevMau")).longValue();
        long prevTodayPosts = toNumber(data.get("prevTodayPosts")).longValue();
        long prevPendingReports = toNumber(data.get("prevPendingReports")).longValue();

        // 覆盖为清洁的 Long 类型
        data.put("totalUsers", totalUsers);
        data.put("todayRegister", todayRegister);
        data.put("dau", dau);
        data.put("mau", mau);
        data.put("totalPosts", totalPosts);
        data.put("todayPosts", todayPosts);
        data.put("totalInteractions", totalInteractions);
        data.put("pendingReports", pendingReports);

        // 计算较昨日趋势（保留 1 位小数，分母为 0 时返回 0）
        data.put("totalUsersTrend", calcTrend(totalUsers, prevTotalUsers));
        data.put("todayRegisterTrend", calcTrend(todayRegister, prevTodayRegister));
        data.put("dauTrend", calcTrend(dau, prevDau));
        data.put("mauTrend", calcTrend(mau, prevMau));
        data.put("todayPostsTrend", calcTrend(todayPosts, prevTodayPosts));
        data.put("pendingReportsTrend", calcTrend(pendingReports, prevPendingReports));

        // 清理中间字段
        data.remove("prevTotalUsers");
        data.remove("prevTodayRegister");
        data.remove("prevDau");
        data.remove("prevMau");
        data.remove("prevTodayPosts");
        data.remove("prevPendingReports");

        return Result.success(data);
    }

    /**
     * 注册趋势
     * <p>功能：按天统计每日新增注册用户数</p>
     *
     * @param range 时间维度: DAY=近30天, WEEK=近12周, MONTH=近12月
     * @return [{date: '2026-06-01', value: 12}, ...]
     */
    @Operation(summary = "注册趋势")
    @GetMapping("/register-trend")
    public Result<List<Map<String, Object>>> registerTrend(
            @RequestParam(defaultValue = "DAY") String range) {
        DateRange dr = calcRange(range);
        List<Map<String, Object>> rows = adminStatsMapper.selectRegisterTrend(dr.start, dr.end);
        fillDateGaps(rows, dr.start, dr.end, "value");
        return Result.success(rows);
    }

    /**
     * 日活/月活趋势
     * <p>功能：按天统计活跃用户数（DAU），返回多系列数据</p>
     *
     * @param range 时间维度
     * @return {dates: [...], series: [{name: 'DAU', data: [...]}, {name: 'MAU', data: [...]}]}
     */
    @Operation(summary = "活跃趋势(DAU/MAU)")
    @GetMapping("/active-trend")
    public Result<Map<String, Object>> activeTrend(
            @RequestParam(defaultValue = "DAY") String range) {
        DateRange dr = calcRange(range);
        List<Map<String, Object>> dauRows = adminStatsMapper.selectDauTrend(dr.start, dr.end);
        List<Map<String, Object>> mauRows = adminStatsMapper.selectMauTrend(dr.start, dr.end);

        List<String> dates = new ArrayList<>();
        List<Long> dauData = new ArrayList<>();
        for (Map<String, Object> row : dauRows) {
            dates.add(toDateString(row.get("date")));
            dauData.add(toNumber(row.get("value")).longValue());
        }

        List<Long> mauData = new ArrayList<>();
        for (Map<String, Object> row : mauRows) {
            mauData.add(toNumber(row.get("value")).longValue());
        }

        Map<String, Object> data = new HashMap<>(4);
        data.put("dates", dates);
        data.put("series", Arrays.asList(
                mapOf("name", "DAU", "data", dauData),
                mapOf("name", "MAU", "data", mauData)
        ));
        return Result.success(data);
    }

    /**
     * 发帖量趋势
     * <p>功能：按天统计每日新增帖子数量</p>
     *
     * @param range 时间维度
     * @return [{date: '2026-06-01', value: 45}, ...]
     */
    @Operation(summary = "发帖趋势")
    @GetMapping("/post-trend")
    public Result<List<Map<String, Object>>> postTrend(
            @RequestParam(defaultValue = "DAY") String range) {
        DateRange dr = calcRange(range);
        List<Map<String, Object>> rows = adminStatsMapper.selectPostTrend(dr.start, dr.end);
        fillDateGaps(rows, dr.start, dr.end, "value");
        return Result.success(rows);
    }

    /**
     * 互动量趋势
     * <p>功能：按天分别统计点赞/评论/收藏三种互动类型的数量</p>
     *
     * @param range 时间维度
     * @return {dates: [...], series: [like/comment/collect 三条线]}
     */
    @Operation(summary = "互动趋势")
    @GetMapping("/interaction-trend")
    public Result<Map<String, Object>> interactionTrend(
            @RequestParam(defaultValue = "DAY") String range) {
        DateRange dr = calcRange(range);
        List<Map<String, Object>> rows = adminStatsMapper.selectInteractionTrend(dr.start, dr.end);

        List<String> dates = new ArrayList<>();
        List<Long> likeList = new ArrayList<>();
        List<Long> commentList = new ArrayList<>();
        List<Long> collectList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            dates.add(toDateString(row.get("date")));
            likeList.add(toNumber(row.get("likeCount")).longValue());
            commentList.add(toNumber(row.get("commentCount")).longValue());
            collectList.add(toNumber(row.get("collectCount")).longValue());
        }

        Map<String, Object> data = new HashMap<>(4);
        data.put("dates", dates);
        data.put("series", Arrays.asList(
                mapOf("name", "点赞", "data", likeList),
                mapOf("name", "评论", "data", commentList),
                mapOf("name", "收藏", "data", collectList)
        ));
        return Result.success(data);
    }

    /**
     * 圈子活跃排行
     * <p>功能：按互动总量降序返回圈子活跃度 TOP N</p>
     *
     * @return [{circleId, name, memberCount, postCount, interactionCount}, ...]
     */
    @Operation(summary = "圈子活跃排行")
    @GetMapping("/circle-active")
    public Result<List<Map<String, Object>>> circleActive() {
        List<Map<String, Object>> list = adminStatsMapper.selectCircleActiveRank(20);
        // 类型转换确保前端拿到数字类型
        for (Map<String, Object> item : list) {
            item.put("circleId", toNumber(item.get("circleId")));
            item.put("memberCount", toNumber(item.get("memberCount")));
            item.put("postCount", toNumber(item.get("postCount")));
            item.put("interactionCount", toNumber(item.get("interactionCount")));
        }
        return Result.success(list);
    }

    /**
     * 举报工单趋势
     * <p>功能：按天统计新增举报工单数量</p>
     *
     * @param range 时间维度
     * @return {dates: [...], series: [{name: '举报数', data: [...]}]}
     */
    @Operation(summary = "举报工单趋势")
    @GetMapping("/report")
    public Result<Map<String, Object>> reportTrend(
            @RequestParam(defaultValue = "DAY") String range) {
        DateRange dr = calcRange(range);
        List<Map<String, Object>> rows = adminStatsMapper.selectReportTrend(dr.start, dr.end);
        fillDateGaps(rows, dr.start, dr.end, "value");

        List<String> dates = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            dates.add(toDateString(row.get("date")));
            values.add(toNumber(row.get("value")).longValue());
        }

        Map<String, Object> data = new HashMap<>(4);
        data.put("dates", dates);
        data.put("series", Collections.singletonList(mapOf("name", "举报数", "data", values)));
        return Result.success(data);
    }

    /**
     * 活跃高峰时段（热力图数据）
     * <p>功能：按小时聚合发帖+评论+点赞行为，返回24h×3类的日均值，支持时间维度切换</p>
     *
     * @param range 时间维度: DAY=近30天, WEEK=近12周, MONTH=近12月
     * @return [{hour: '08:00', postCount, commentCount, likeCount, total}, ...]
     */
    @Operation(summary = "活跃高峰时段(热力图)")
    @GetMapping("/peak-hours")
    public Result<List<Map<String, Object>>> peakHours(
            @RequestParam(defaultValue = "DAY") String range) {
        DateRange dr = calcRange(range);
        LocalDate start = LocalDate.parse(dr.start);
        LocalDate end = LocalDate.parse(dr.end);
        int dayCount = (int) ChronoUnit.DAYS.between(start, end) + 1;
        List<Map<String, Object>> rows = adminStatsMapper.selectPeakHours(dr.start, dr.end, dayCount);
        // 格式化 hour 字段，去掉尾部（如 "08:00-09:00" → "08:00"）
        for (Map<String, Object> row : rows) {
            String hour = (String) row.get("hour");
            if (hour != null && hour.contains("-")) {
                row.put("hour", hour.substring(0, 5));
            }
        }
        return Result.success(rows);
    }

    /**
     * 帖子审核漏斗
     * <p>功能：返回帖子审核状态的实时快照及按天趋势</p>
     *
     * @param range 时间维度
     * @return {snapshot: {totalPosts, pendingCount, approvedCount, rejectedCount, passRate}, trend: [...]}
     */
    @Operation(summary = "帖子审核漏斗")
    @GetMapping("/audit-funnel")
    public Result<Map<String, Object>> auditFunnel(
            @RequestParam(defaultValue = "DAY") String range) {
        DateRange dr = calcRange(range);
        Map<String, Object> snapshot = adminStatsMapper.selectAuditFunnel();
        List<Map<String, Object>> trend = adminStatsMapper.selectAuditTrend(dr.start, dr.end);
        // 数值类型安全转换
        for (String key : new String[]{"totalPosts", "pendingCount", "approvedCount", "rejectedCount"}) {
            snapshot.put(key, toNumber(snapshot.get(key)).longValue());
        }
        snapshot.put("passRate", toNumber(snapshot.get("passRate")).doubleValue());

        Map<String, Object> data = new HashMap<>(4);
        data.put("snapshot", snapshot);
        data.put("trend", trend);
        return Result.success(data);
    }

    /**
     * AI 运营日报
     * <p>功能：基于平台真实运营数据生成 AI 辅助运营分析报告，包括健康评分、违规分布、高峰时段、AI 建议、风险等级</p>
     * <p>注意事项：违规类型/高峰时段/风险等级均从数据库实时统计；运营建议由本地私有化 AI 大模型生成；AI 调用失败时降级为规则建议</p>
     *
     * @return AI 日报结构体
     */
    @Operation(summary = "AI 运营日报")
    @GetMapping("/ai-report")
    public Result<Map<String, Object>> aiReport() {
        // 1. 从数据库获取真实核心指标
        Map<String, Object> core = adminStatsMapper.selectCoreMetrics();
        long totalUsers = toNumber(core.get("totalUsers")).longValue();
        long todayPosts = toNumber(core.get("todayPosts")).longValue();
        long pendingReports = toNumber(core.get("pendingReports")).longValue();
        long dau = toNumber(core.get("dau")).longValue();
        long totalPosts = toNumber(core.get("totalPosts")).longValue();
        long totalInteractions = toNumber(core.get("totalInteractions")).longValue();

        // 2. 健康评分算法（基于真实数据）
        double userRatio = totalUsers > 0 ? (double) dau / Math.min(totalUsers, 10000) : 0;
        double postScore = Math.min(todayPosts / 10.0, 1.0) * 100;
        double reportScore = Math.max(0, 100 - pendingReports * 5);
        int healthScore = (int) Math.round(Math.min(userRatio * 40 + postScore * 0.3 + reportScore * 0.3, 100));

        // 3. 违规类型分布（从 report 表真实统计）
        List<Map<String, Object>> violationRows = adminStatsMapper.selectViolationTypeDist();
        List<Map<String, Object>> violationTypes = new ArrayList<>();
        for (Map<String, Object> row : violationRows) {
            violationTypes.add(mapOf("type", row.get("type"), "count", toNumber(row.get("count"))));
        }
        if (violationTypes.isEmpty()) {
            violationTypes.add(mapOf("type", "暂无违规", "count", 0));
        }

        // 4. 活跃高峰时段（从 post/comment/like_collect 表按小时聚合今日数据）
        String todayStr = LocalDate.now().format(DATE_FMT);
        List<Map<String, Object>> peakHourRows = adminStatsMapper.selectPeakHours(todayStr, todayStr, 1);
        // 取 total 前 5 的时段作为高峰时段
        peakHourRows.sort((a, b) -> Long.compare(toNumber(b.get("total")).longValue(), toNumber(a.get("total")).longValue()));
        List<Map<String, Object>> peakHours = new ArrayList<>();
        int limit = Math.min(5, peakHourRows.size());
        for (int i = 0; i < limit; i++) {
            Map<String, Object> row = peakHourRows.get(i);
            peakHours.add(mapOf("hour", row.get("hour"), "count", toNumber(row.get("total"))));
        }
        if (peakHours.isEmpty()) {
            peakHours.add(mapOf("hour", "暂无数据", "count", 0));
        }

        // 5. 风险等级分布（从 report 表 ai_risk_level 真实统计）
        List<Map<String, Object>> riskRows = adminStatsMapper.selectRiskLevelDist();
        Map<String, Long> riskMap = new LinkedHashMap<>();
        for (Map<String, Object> row : riskRows) {
            riskMap.put((String) row.get("level"), toNumber(row.get("count")).longValue());
        }
        // 确保 HIGH / MEDIUM / LOW 三个等级都有值（没有的补 0）
        List<Map<String, Object>> riskDistribution = Arrays.asList(
                mapOf("level", "HIGH", "count", riskMap.getOrDefault("HIGH", 0L)),
                mapOf("level", "MEDIUM", "count", riskMap.getOrDefault("MEDIUM", 0L)),
                mapOf("level", "LOW", "count", riskMap.getOrDefault("LOW", 0L))
        );

        // 6. AI 运营建议（调用真实 AI 大模型生成，失败时降级为规则建议）
        List<String> suggestions = generateAiSuggestions(
                healthScore, totalUsers, dau, todayPosts, pendingReports, totalPosts, totalInteractions);

        // 7. 组装返回结果
        Map<String, Object> data = new HashMap<>(10);
        data.put("reportDate", LocalDate.now().toString());
        data.put("healthScore", healthScore);
        data.put("violationTypes", violationTypes);
        data.put("peakHours", peakHours);
        data.put("suggestions", suggestions);
        data.put("riskDistribution", riskDistribution);
        // 标识当前使用的 AI 模式，前端据此展示不同的提示文案
        data.put("aiMode", aiConfig.getMode());

        return Result.success(data);
    }

    /**
     * 调用 AI 大模型生成运营优化建议
     * <p>功能：将平台核心指标数据组装为提示词，调用 AiContext.smartQa() 由本地私有化 AI 模型生成个性化运营建议</p>
     * <p>异常处理：AI 调用失败时降级为基于规则的通用建议，确保接口不因 AI 异常而报错</p>
     *
     * @param healthScore      社区健康评分（0-100）
     * @param totalUsers       总用户数
     * @param dau              日活跃用户数
     * @param todayPosts       今日发帖数
     * @param pendingReports   待处理工单数
     * @param totalPosts       总帖子数
     * @param totalInteractions 总互动量
     * @return 运营建议列表（通常 2-4 条）
     */
    private List<String> generateAiSuggestions(int healthScore, long totalUsers, long dau,
                                               long todayPosts, long pendingReports,
                                               long totalPosts, long totalInteractions) {
        String fallbackMessage = aiConfig.getFallback().getMessage();
        try {
            // 构建包含真实运营数据的提示词
            String prompt = String.format(
                    "你是 MintHive 兴趣社交平台的运营数据分析专家。根据以下今日真实运营数据，给出 3-4 条简洁具体的运营优化建议（每条不超过30字），直接列出建议内容，不要解释：\n" +
                    "- 社区健康评分: %d/100\n" +
                    "- 总用户数: %d\n" +
                    "- 日活(DAU): %d\n" +
                    "- 今日发帖: %d\n" +
                    "- 待处理工单: %d\n" +
                    "- 总帖子数: %d\n" +
                    "- 总互动量: %d\n" +
                    "\n请用中文回答，每条建议单独一行，以数字序号开头。",
                    healthScore, totalUsers, dau, todayPosts, pendingReports, totalPosts, totalInteractions
            );

            String aiResponse = aiContext.smartQa(prompt);

            // AI 服务内部降级（cloud 调用失败返回配置的 fallback 消息），视为未生成
            if (aiResponse == null || aiResponse.equals(fallbackMessage)) {
                log.warn("[AI日报] AI 返回降级消息，使用规则建议");
                return fallbackSuggestions(healthScore, todayPosts, pendingReports, totalUsers);
            }

            // 解析 AI 回复为建议列表（按换行或序号分割）
            List<String> result = new ArrayList<>();
            String[] lines = aiResponse.split("\n");
            for (String line : lines) {
                String trimmed = line.trim()
                        .replaceAll("^[\\d\\.、\\s]+", "")  // 去掉开头的序号
                        .trim();
                if (!trimmed.isEmpty() && trimmed.length() > 2) {
                    result.add(trimmed);
                }
            }

            if (!result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("[AI日报] AI 生成建议失败，降级为规则建议: {}", e.getMessage());
        }

        return fallbackSuggestions(healthScore, todayPosts, pendingReports, totalUsers);
    }

    /**
     * 规则降级运营建议（AI 不可用时使用）
     * <p>功能：基于健康评分和关键指标的简单规则生成兜底建议</p>
     *
     * @param healthScore    健康评分
     * @param todayPosts     今日发帖数
     * @param pendingReports 待处理工单数
     * @param totalUsers     总用户数
     * @return 降级建议列表
     */
    private List<String> fallbackSuggestions(int healthScore, long todayPosts,
                                             long pendingReports, long totalUsers) {
        List<String> suggestions = new ArrayList<>();
        if (healthScore >= 80) {
            suggestions.add("社区整体运行健康，用户活跃度高");
            suggestions.add("建议继续推进优质内容扶持计划");
        } else if (healthScore >= 60) {
            suggestions.add("社区活跃度中等，建议加强内容推荐策略");
            suggestions.add("关注晚间高峰时段的内容审核效率");
        } else {
            suggestions.add("社区活跃度偏低，建议开展拉新活动");
            suggestions.add("待处理工单较多，建议增加审核人力");
        }
        if (pendingReports > 10) {
            suggestions.add("举报积压严重，建议优先处理高风险工单");
        }
        if (todayPosts < 5 && totalUsers > 50) {
            suggestions.add("今日发帖量偏低，可考虑推送话题活动激励用户");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("各项指标运行正常，继续保持监控");
        }
        return suggestions;
    }

    /**
     * 导出统计报表（Excel）
     * <p>功能：导出当前核心指标数据为 Excel 文件，供离线分析使用</p>
     *
     * @param range 时间维度
     * @return Excel 文件流
     */
    @Operation(summary = "导出Excel报表")
    @GetMapping("/export")
    public void exportStats(@RequestParam(defaultValue = "DAY") String range,
                            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=stats_"
                + LocalDate.now() + "_" + range + ".xlsx");

        // 使用 Apache POI 生成简单 Excel
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("运营数据");

            // 表头
            org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
            String[] headers = {"指标", "数值"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // 核心指标数据
            Map<String, Object> core = adminStatsMapper.selectCoreMetrics();
            String[][] metrics = {
                    {"总用户数", toNumber(core.get("totalUsers")).toString()},
                    {"今日注册", toNumber(core.get("todayRegister")).toString()},
                    {"日活(DAU)", toNumber(core.get("dau")).toString()},
                    {"月活(MAU)", toNumber(core.get("mau")).toString()},
                    {"总帖子数", toNumber(core.get("totalPosts")).toString()},
                    {"今日发帖", toNumber(core.get("todayPosts")).toString()},
                    {"总互动量", toNumber(core.get("totalInteractions")).toString()},
                    {"待处理工单", toNumber(core.get("pendingReports")).toString()}
            };
            for (int i = 0; i < metrics.length; i++) {
                org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(metrics[i][0]);
                row.createCell(1).setCellValue(metrics[i][1]);
            }

            // 列宽自适应
            for (int i = 0; i < 2; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(response.getOutputStream());
        }
    }

    /**
     * 计算较昨日变化百分比，保留 1 位小数
     * @param today    今日值
     * @param yesterday 昨日值
     * @return 百分比（如 12.3, -5.0），昨日为 0 时返回 0
     */
    private double calcTrend(long today, long yesterday) {
        if (yesterday == 0) return 0;
        return Math.round((double) (today - yesterday) / yesterday * 100 * 10) / 10.0;
    }

    // ==================== 内部工具方法 ====================

    /**
     * 将对象安全转换为 Number
     * @param val 可能是 BigDecimal/Long/Integer 的值
     * @return Number 对象
     */
    private Number toNumber(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number) return (Number) val;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 根据 range 参数计算起止日期
     * 功能：DAY→近30天，WEEK→近12周(84天)，MONTH→近12月(360天)
     * @param range 时间维度字符串
     * @return 起止日期范围
     */
    private DateRange calcRange(String range) {
        LocalDate now = LocalDate.now();
        int days;
        switch (range.toUpperCase()) {
            case "WEEK":
                days = 84;
                break;
            case "MONTH":
                days = 360;
                break;
            case "DAY":
            default:
                days = 30;
                break;
        }
        LocalDate start = now.minusDays(days);
        return new DateRange(start.format(DATE_FMT), now.format(DATE_FMT));
    }

    /**
     * 填充日期序列中的空缺（没有数据的日期补 value=0）
     * 功能：确保折线图 X 轴连续无断点
     * @param rows   原始查询结果
     * @param start  起始日期
     * @param end    结束日期
     * @param valueKey 数值字段名
     */
    private void fillDateGaps(List<Map<String, Object>> rows, String start, String end, String valueKey) {
        if (rows == null || rows.isEmpty()) {
            rows = new ArrayList<>();
        }
        Set<String> existingDates = new HashSet<>();
        for (Map<String, Object> row : rows) {
            existingDates.add(toDateString(row.get("date")));
        }

        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        List<Map<String, Object>> filled = new ArrayList<>();

        // 按 date 排序后逐日填充
        Map<String, Map<String, Object>> rowMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            rowMap.put(toDateString(row.get("date")), row);
        }

        LocalDate d = startDate;
        while (!d.isAfter(endDate)) {
            Map<String, Object> r = rowMap.get(d.format(DATE_FMT));
            if (r != null) {
                filled.add(r);
            } else {
                filled.add(mapOf("date", d.format(DATE_FMT), valueKey, 0));
            }
            d = d.plusDays(1);
        }
        rows.clear();
        rows.addAll(filled);
    }

    /**
     * 安全地将日期对象转换为字符串
     * 功能：兼容 java.sql.Date、java.time.LocalDate 等多种日期类型返回值
     * @param val 日期对象（可能是 String/Date/LocalDate）
     * @return yyyy-MM-dd 格式的字符串
     */
    private String toDateString(Object val) {
        if (val == null) return "";
        if (val instanceof String) return (String) val;
        if (val instanceof java.sql.Date) return ((java.sql.Date) val).toString();
        if (val instanceof java.sql.Timestamp) return ((java.sql.Timestamp) val).toLocalDateTime().toLocalDate().toString();
        if (val instanceof java.time.LocalDate) return ((java.time.LocalDate) val).toString();
        return val.toString();
    }

    /**
     * 快速构建 Map 工具方法
     */
    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(K key1, V val1, Object... rest) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(key1, val1);
        for (int i = 0; i < rest.length; i += 2) {
            if (i + 1 < rest.length) {
                map.put((K) rest[i], (V) rest[i + 1]);
            }
        }
        return map;
    }

    /** 日期范围内部类 */
    private static class DateRange {
        String start;
        String end;
        DateRange(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }
}
