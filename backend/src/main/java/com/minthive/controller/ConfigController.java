package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.entity.Banner;
import com.minthive.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置公开接口控制器
 *
 * <p>功能描述：提供用户端轮播图等配置查询接口（无需登录）</p>
 * <p>访问控制：所有接口已在 WebMvcConfig 中配置为公开访问</p>
 */
@Tag(name = "系统配置-公开接口")
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final BannerService bannerService;

    /**
     * 获取首页轮播图列表（仅返回启用+排序）
     *
     * @return 启用状态的轮播图列表，按排序升序排列
     */
    @Operation(summary = "首页轮播图列表")
    @GetMapping("/banners")
    public Result<List<Banner>> getBanners() {
        return Result.success(bannerService.listActive());
    }
}
