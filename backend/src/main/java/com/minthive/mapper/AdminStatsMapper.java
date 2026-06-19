package com.minthive.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 管理后台统计 Mapper
 * <p>功能描述：提供数据大屏所需的各类聚合统计 SQL 查询</p>
 * <p>注意事项：所有查询均排除逻辑删除的数据，日期范围由调用方通过参数控制</p>
 */
@Mapper
public interface AdminStatsMapper {

    /**
     * 核心指标查询
     * 功能：一次性返回平台核心运营指标（用户数、今日注册、日活、月活、帖子数等）
     * @return 包含各项指标的 Map
     */
    Map<String, Object> selectCoreMetrics();

    /**
     * 注册趋势（按天分组）
     * 功能：按日期维度统计每日新增注册用户数
     * @param startDate 起始日期（含），格式 yyyy-MM-dd
     * @param endDate   结束日期（含），格式 yyyy-MM-dd
     * @return 日期-数量列表 [{date: '2026-06-01', value: 12}, ...]
     */
    List<Map<String, Object>> selectRegisterTrend(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);

    /**
     * 日活趋势（按天分组，活跃用户数）
     * 功能：按日期统计有发帖/评论/点赞行为的独立用户数
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 日期-DAU列表
     */
    List<Map<String, Object>> selectDauTrend(@Param("startDate") String startDate,
                                             @Param("endDate") String endDate);

    /**
     * 发帖趋势（按天分组）
     * 功能：按日期统计每日新增帖子数
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 日期-帖子数列表
     */
    List<Map<String, Object>> selectPostTrend(@Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    /**
     * 互动趋势（按天分组，分类型统计）
     * 功能：按日期分别统计点赞/评论/收藏的互动量
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 日期-分类互动量列表，包含 likeCount/commentCount/collectCount
     */
    List<Map<String, Object>> selectInteractionTrend(@Param("startDate") String startDate,
                                                     @Param("endDate") String endDate);

    /**
     * 圈子活跃排行
     * 功能：统计每个圈子的成员数、帖子数、总互动量，按互动量降序排列
     * @param limit 返回条数上限
     * @return 圈子活跃排行列表
     */
    List<Map<String, Object>> selectCircleActiveRank(@Param("limit") int limit);

    /**
     * 举报工单趋势（按天分组）
     * 功能：按日期统计每日新增举报工单数
     * @param startDate 起始日期
     * @param endDate   结束日期
     * @return 日期-工单数列表
     */
    List<Map<String, Object>> selectReportTrend(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate);

    /**
     * 违规类型分布统计
     * 功能：从举报工单表按 report_type 分组统计各类型违规数量
     * @return [{type: '广告引流', count: 5}, ...]
     */
    List<Map<String, Object>> selectViolationTypeDist();

    /**
     * 用户活跃高峰时段统计
     * 功能：按小时聚合今日发帖+评论+点赞行为，找出活跃高峰时段
     * @return [{hour: '08:00-09:00', postCount: 10, commentCount: 5, likeCount: 20, total: 35}, ...]
     */
    List<Map<String, Object>> selectPeakHours();

    /**
     * 举报风险等级分布统计
     * 功能：按 ai_risk_level 分组统计待处理工单的风险等级分布
     * @return [{level: 'HIGH', count: 3}, ...]
     */
    List<Map<String, Object>> selectRiskLevelDist();
}
