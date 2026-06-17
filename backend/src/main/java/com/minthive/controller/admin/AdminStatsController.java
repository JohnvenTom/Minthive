package com.minthive.controller.admin;

import com.minthive.common.Result;
import com.minthive.mapper.AdminStatsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsMapper adminStatsMapper;

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 核心指标总览
     * <p>功能：返回平台当前的核心运营数据，包括用户总数、今日注册、日活/月活、帖子数、互动量、待处理工单</p>
     *
     * @return 包含 totalUsers/todayRegister/dau/mau/totalPosts/todayPosts/totalInteractions/pendingReports 的 Map
     */
    @Operation(summary = "核心指标")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        Map<String, Object> data = adminStatsMapper.selectCoreMetrics();
        // 确保数值字段为 Long/Integer 类型（避免前端 JSON 数字精度问题）
        data.put("totalUsers", toNumber(data.get("totalUsers")));
        data.put("todayRegister", toNumber(data.get("todayRegister")));
        data.put("dau", toNumber(data.get("dau")));
        data.put("mau", toNumber(data.get("mau")));
        data.put("totalPosts", toNumber(data.get("totalPosts")));
        data.put("todayPosts", toNumber(data.get("todayPosts")));
        data.put("totalInteractions", toNumber(data.get("totalInteractions")));
        data.put("pendingReports", toNumber(data.get("pendingReports")));
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

        List<String> dates = new ArrayList<>();
        List<Long> dauData = new ArrayList<>();
        for (Map<String, Object> row : dauRows) {
            dates.add(toDateString(row.get("date")));
            dauData.add(toNumber(row.get("value")).longValue());
        }

        // MAU 用滚动 30 天窗口计算（简化为 DAU 的累计去重近似）
        List<Long> mauData = new ArrayList<>();
        for (int i = 0; i < dauData.size(); i++) {
            long sum = 0;
            int start = Math.max(0, i - 29);
            for (int j = start; j <= i; j++) {
                sum += dauData.get(j);
            }
            mauData.add(sum); // 近似值，实际 MAU 应该用独立用户去重
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
     * AI 运营日报
     * <p>功能：基于平台数据生成 AI 辅助运营分析报告，包括健康评分、违规分布、高峰时段、建议等</p>
     *
     * @return AI 日报结构体
     */
    @Operation(summary = "AI 运营日报")
    @GetMapping("/ai-report")
    public Result<Map<String, Object>> aiReport() {
        Map<String, Object> core = adminStatsMapper.selectCoreMetrics();
        long totalUsers = toNumber(core.get("totalUsers")).longValue();
        long todayPosts = toNumber(core.get("todayPosts")).longValue();
        long pendingReports = toNumber(core.get("pendingReports")).longValue();
        long dau = toNumber(core.get("dau")).longValue();

        // 健康评分算法：(DAU/总用户*40 + 今日发帖基数分*30 + 工单压力*30)，满分100
        double userRatio = totalUsers > 0 ? (double) dau / Math.min(totalUsers, 10000) : 0;
        double postScore = Math.min(todayPosts / 10.0, 1.0) * 100;
        double reportScore = Math.max(0, 100 - pendingReports * 5);
        int healthScore = (int) Math.round(Math.min(userRatio * 40 + postScore * 0.3 + reportScore * 0.3, 100));

        Map<String, Object> data = new HashMap<>(8);
        data.put("reportDate", LocalDate.now().toString());
        data.put("healthScore", healthScore);

        // 违规类型分布（模拟数据，后续对接 AI 审核服务真实统计）
        data.put("violationTypes", Arrays.asList(
                mapOf("type", "低俗色情", "count", 2),
                mapOf("type", "广告引流", "count", 5),
                mapOf("type", "人身攻击", "count", 3),
                mapOf("type", "违法内容", "count", 0),
                mapOf("type", "抄袭搬运", "count", 1)
        ));

        // 活跃高峰时段（模拟）
        data.put("peakHours", Arrays.asList(
                mapOf("hour", "08:00-10:00", "count", 120),
                mapOf("hour", "12:00-14:00", "count", 256),
                mapOf("hour", "18:00-20:00", "count", 389),
                mapOf("hour", "20:00-22:00", "count", 445),
                mapOf("hour", "22:00-24:00", "count", 210)
        ));

        // AI 建议
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
        data.put("suggestions", suggestions);

        // 风险等级分布
        data.put("riskDistribution", Arrays.asList(
                mapOf("level", "HIGH", "count", pendingReports > 5 ? 3 : 0),
                mapOf("level", "MEDIUM", "count", pendingReports > 2 ? 5 : 2),
                mapOf("level", "LOW", "count", Math.max(pendingReports - 8, 8))
        ));

        return Result.success(data);
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
