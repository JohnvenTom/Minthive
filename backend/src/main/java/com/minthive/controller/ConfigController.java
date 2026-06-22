package com.minthive.controller;

import com.minthive.common.Result;
import com.minthive.controller.admin.AdminConfigController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    private final com.minthive.controller.admin.AdminConfigController adminConfigController;

    /**
     * 获取首页轮播图列表（仅返回启用状态）
     *
     * @return 启用状态的轮播图列表，按排序升序排列
     */
    @Operation(summary = "首页轮播图列表")
    @GetMapping("/banners")
    public Result<List<Map<String, Object>>> getBanners() {
        // 通过 admin 控制器获取全部轮播数据，再过滤出启用的
        var result = adminConfigController.getBanners();
        List<Map<String, Object>> allBanners = result.getData();
        if (allBanners == null || allBanners.isEmpty()) {
            return Result.success(List.of());
        }
        // 过滤启用状态 + 按排序升序
        List<Map<String, Object>> activeBanners = allBanners.stream()
                .filter(b -> "ACTIVE".equals(b.get("status")))
                .sorted((a, b) -> {
                    Number sa = a.get("sort") instanceof Number ? (Number) a.get("sort") : 0;
                    Number sb = b.get("sort") instanceof Number ? (Number) b.get("sort") : 0;
                    return sa.intValue() - sb.intValue();
                })
                .toList();
        return Result.success(activeBanners);
    }
}
