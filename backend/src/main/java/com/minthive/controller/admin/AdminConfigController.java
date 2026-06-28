package com.minthive.controller.admin;

import com.minthive.common.Result;
import com.minthive.entity.Announcement;
import com.minthive.entity.Banner;
import com.minthive.service.AnnouncementService;
import com.minthive.service.BannerService;
import com.minthive.service.SystemMsgService;
import com.minthive.websocket.WebSocketServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台-系统配置控制器
 * <p>功能描述：公告管理、Banner管理</p>
 */
@Tag(name = "管理后台-系统配置")
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SystemMsgService systemMsgService;
    private final AnnouncementService announcementService;
    private final BannerService bannerService;
    private final WebSocketServer webSocketServer;

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
