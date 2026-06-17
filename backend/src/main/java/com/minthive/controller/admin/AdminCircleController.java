package com.minthive.controller.admin;

import com.minthive.common.Result;
import com.minthive.entity.Circle;
import com.minthive.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台-圈子管理控制器
 */
@Tag(name = "管理后台-圈子管理")
@RestController
@RequestMapping("/api/admin/circle")
@RequiredArgsConstructor
public class AdminCircleController {

    private final CircleService circleService;

    /**
     * 更新圈子信息
     *
     * @param circle 圈子实体
     * @return 更新后的圈子
     */
    @Operation(summary = "更新圈子信息")
    @PutMapping
    public Result<Circle> update(@RequestBody Circle circle) {
        return Result.success(circleService.update(circle));
    }

    /**
     * 下架圈子
     *
     * @param id 圈子ID
     * @return 操作结果
     */
    @Operation(summary = "下架圈子")
    @PutMapping("/offline/{id}")
    public Result<Void> offline(@PathVariable Long id) {
        Circle update = new Circle();
        update.setId(id);
        update.setStatus(0);
        circleService.update(update);
        return Result.success();
    }
}
