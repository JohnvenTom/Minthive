package com.minthive.controller.admin;

import com.minthive.common.Result;
import com.minthive.entity.Announcement;
import com.minthive.entity.Banner;
import com.minthive.service.AnnouncementService;
import com.minthive.service.BannerService;
import com.minthive.service.SensitiveWordService;
import com.minthive.service.SystemMsgService;
import com.minthive.websocket.WebSocketServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 管理后台-系统配置控制器
 * <p>功能描述：公告管理、Banner管理、平台规则、AI开关、敏感词检测等</p>
 */
@Tag(name = "管理后台-系统配置")
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SensitiveWordService sensitiveWordService;
    private final SystemMsgService systemMsgService;
    private final AnnouncementService announcementService;
    private final BannerService bannerService;
    private final WebSocketServer webSocketServer;

    /** 内存存储规则/AI开关（Banner已迁移至数据库） */
    private static Map<String, Object> PLATFORM_RULES = Collections.synchronizedMap(new LinkedHashMap<>());
    private static Map<String, Object> AI_SWITCH = Collections.synchronizedMap(new LinkedHashMap<>());

    static {
        // 默认规则
        PLATFORM_RULES.put("communityRules", "1. 禁止发布违法违规内容\n2. 尊重他人，禁止人身攻击\n3. 禁止广告引流\n4. 保护隐私，禁止人肉搜索");
        PLATFORM_RULES.put("copyrightNotice", "MintHive 平台保留对违规内容的处置权");
        // 默认AI开关
        AI_SWITCH.put("aiReviewEnabled", true);
        AI_SWITCH.put("aiReplyEnabled", true);
        AI_SWITCH.put("aiReportEnabled", true);
        AI_SWITCH.put("riskThreshold", "MEDIUM");
    }

    // ==================== 公告管理（持久化至数据库）====================

    /**
     * 公告列表
     *
     * @param current 当前页码，默认1
     * @param size    每页大小，默认20
     * @return 公告分页数据
     */
    @Operation(summary = "公告列表")
    @GetMapping("/announcements")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Announcement>> getAnnouncements(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(announcementService.page(current, size));
    }

    /**
     * 创建/更新公告
     *
     * @param announcement 公告实体（id可选，有则更新）
     * @return 操作结果
     */
    @Operation(summary = "保存公告")
    @PostMapping("/announcement")
    public Result<Announcement> saveAnnouncement(@RequestBody Announcement announcement) {
        if (announcement.getId() != null) {
            return Result.success(announcementService.update(announcement));
        }
        return Result.success(announcementService.add(announcement));
    }

    /**
     * 删除公告
     *
     * @param id 公告ID
     * @return 操作结果
     */
    @Operation(summary = "删除公告")
    @DeleteMapping("/announcement")
    public Result<Void> deleteAnnouncement(@RequestParam Long id) {
        announcementService.deleteById(id);
        return Result.success();
    }

    // ==================== Banner管理（持久化至数据库）====================

    @Operation(summary = "Banner列表")
    @GetMapping("/banners")
    public Result<List<Banner>> getBanners() {
        return Result.success(bannerService.listAll());
    }

    @Operation(summary = "保存Banner")
    @PostMapping("/banner")
    public Result<Banner> saveBanner(@RequestBody Banner banner) {
        return Result.success(bannerService.save(banner));
    }

    @Operation(summary = "删除Banner")
    @DeleteMapping("/banner")
    public Result<Void> deleteBanner(@RequestParam Long id) {
        bannerService.deleteById(id);
        return Result.success();
    }

    // ==================== 平台规则 ====================

    /**
     * 获取平台规则
     *
     * @return 规则内容
     */
    @Operation(summary = "平台规则")
    @GetMapping("/rules")
    public Result<Map<String, Object>> getPlatformRules() {
        return Result.success(PLATFORM_RULES);
    }

    /**
     * 更新平台规则
     *
     * @param data 规则数据
     * @return 操作结果
     */
    @Operation(summary = "更新平台规则")
    @PutMapping("/rules")
    public Result<Map<String, Object>> updatePlatformRules(@RequestBody Map<String, Object> data) {
        PLATFORM_RULES.clear();
        PLATFORM_RULES.putAll(data);
        return Result.success(data);
    }

    // ==================== AI 开关 ====================

    /**
     * 获取AI功能开关配置
     *
     * @return AI开关状态
     */
    @Operation(summary = "AI开关配置")
    @GetMapping("/ai-switch")
    public Result<Map<String, Object>> getAiSwitch() {
        return Result.success(AI_SWITCH);
    }

    /**
     * 更新AI功能开关
     *
     * @param data 开关配置
     * @return 操作结果
     */
    @Operation(summary = "更新AI开关")
    @PutMapping("/ai-switch")
    public Result<Map<String, Object>> updateAiSwitch(@RequestBody Map<String, Object> data) {
        AI_SWITCH.clear();
        AI_SWITCH.putAll(data);
        return Result.success(AI_SWITCH);
    }

    // ==================== 敏感词（原有接口保留）====================

    /**
     * 敏感词检测
     *
     * @param text 待检测文本
     * @return true 包含敏感词
     */
    @Operation(summary = "敏感词检测")
    @GetMapping("/sensitive/check")
    public Result<Boolean> checkSensitive(@RequestParam String text) {
        return Result.success(sensitiveWordService.contains(text));
    }

    /**
     * 批量导入敏感词
     *
     * @param file 敏感词文件
     * @return 导入数量
     */
    @Operation(summary = "批量导入敏感词")
    @PostMapping("/sensitive/import")
    public Result<Integer> importSensitive(@RequestParam("file") MultipartFile file) {
        return Result.success(sensitiveWordService.importWords(file));
    }

    /**
     * 发布系统公告（同时持久化到数据库 + WebSocket推送）
     *
     * @param title   公告标题
     * @param content 公告内容
     * @return 操作结果
     */
    @Operation(summary = "发布系统公告")
    @PostMapping("/notice")
    public Result<Announcement> publishNotice(
            @RequestParam String title,
            @RequestParam String content) {
        // 持久化到数据库
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setStatus(1);
        Announcement saved = announcementService.add(announcement);
        // 通过 WebSocket 实时推送给在线用户
        systemMsgService.pushSystemNotice("【系统公告】" + title + ": " + content);
        return Result.success(saved);
    }
}
