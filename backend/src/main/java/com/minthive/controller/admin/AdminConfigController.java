package com.minthive.controller.admin;

import com.minthive.common.Result;
import com.minthive.service.SensitiveWordService;
import com.minthive.service.SystemMsgService;
import com.minthive.websocket.WebSocketServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理后台-系统配置控制器
 */
@Tag(name = "管理后台-系统配置")
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SensitiveWordService sensitiveWordService;
    private final SystemMsgService systemMsgService;
    private final WebSocketServer webSocketServer;

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
     * 发布系统公告
     *
     * @param content 公告内容
     * @return 操作结果
     */
    @Operation(summary = "发布系统公告")
    @PostMapping("/notice")
    public Result<Void> publishNotice(@RequestParam String content) {
        systemMsgService.pushSystemNotice(content);
        return Result.success();
    }
}
