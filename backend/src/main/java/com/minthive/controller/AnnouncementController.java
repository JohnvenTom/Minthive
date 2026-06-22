package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.entity.Announcement;
import com.minthive.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统公告公开接口控制器
 *
 * <p>功能描述：提供用户端公告查询接口（无需登录），用于首页横幅和消息中心公告列表</p>
 * <p>访问控制：所有接口已在 WebMvcConfig 中配置为公开访问</p>
 */
@Tag(name = "系统公告-公开接口")
@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 获取最新一条启用公告（首页横幅用）
     *
     * @return 最新启用的公告，无则返回空数据
     */
    @Operation(summary = "最新公告")
    @GetMapping("/latest")
    public Result<Announcement> getLatest() {
        return Result.success(announcementService.getLatestActive());
    }

    /**
     * 获取所有启用公告列表（消息中心公告页签用）
     *
     * @return 启用状态公告列表，按发布时间倒序
     */
    @Operation(summary = "公告列表")
    @GetMapping("/list")
    public Result<List<Announcement>> getList() {
        return Result.success(announcementService.listActive());
    }
}
