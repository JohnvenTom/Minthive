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

import java.util.*;

/**
 * 管理后台-系统配置控制器
 * <p>功能描述：公告管理、Banner管理、平台规则、运营人员管理、AI开关、敏感词检测等</p>
 */
@Tag(name = "管理后台-系统配置")
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final SensitiveWordService sensitiveWordService;
    private final SystemMsgService systemMsgService;
    private final WebSocketServer webSocketServer;

    /** 内存存储公告/Banner/规则（生产环境应使用数据库） */
    private static final List<Map<String, Object>> ANNOUNCEMENTS = Collections.synchronizedList(new ArrayList<>());
    private static final List<Map<String, Object>> BANNERS = Collections.synchronizedList(new ArrayList<>());
    private static Map<String, Object> PLATFORM_RULES = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final List<Map<String, Object>> OPERATORS = Collections.synchronizedList(new ArrayList<>());
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

    // ==================== 公告管理 ====================

    /**
     * 公告列表
     *
     * @return 公告数组
     */
    @Operation(summary = "公告列表")
    @GetMapping("/announcements")
    public Result<List<Map<String, Object>>> getAnnouncements() {
        return Result.success(ANNOUNCEMENTS);
    }

    /**
     * 创建/更新公告
     *
     * @param data 公告数据（id可选，有则更新）
     * @return 操作结果
     */
    @Operation(summary = "保存公告")
    @PostMapping("/announcement")
    public Result<Map<String, Object>> saveAnnouncement(@RequestBody Map<String, Object> data) {
        if (data.containsKey("id")) {
            for (int i = 0; i < ANNOUNCEMENTS.size(); i++) {
                if (Objects.equals(ANNOUNCEMENTS.get(i).get("id"), data.get("id"))) {
                    ANNOUNCEMENTS.set(i, data);
                    return Result.success(data);
                }
            }
        } else {
            data.put("id", System.currentTimeMillis());
            data.put("createTime", new Date());
            ANNOUNCEMENTS.add(0, data);
        }
        return Result.success(data);
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
        ANNOUNCEMENTS.removeIf(a -> Objects.equals(a.get("id"), id));
        return Result.success();
    }

    // ==================== Banner管理 ====================

    /**
     * Banner列表
     *
     * @return Banner数组
     */
    @Operation(summary = "Banner列表")
    @GetMapping("/banners")
    public Result<List<Map<String, Object>>> getBanners() {
        return Result.success(BANNERS);
    }

    /**
     * 创建/更新Banner
     *
     * @param data Banner数据
     * @return 操作结果
     */
    @Operation(summary = "保存Banner")
    @PostMapping("/banner")
    public Result<Map<String, Object>> saveBanner(@RequestBody Map<String, Object> data) {
        if (data.containsKey("id")) {
            for (int i = 0; i < BANNERS.size(); i++) {
                if (Objects.equals(BANNERS.get(i).get("id"), data.get("id"))) {
                    BANNERS.set(i, data);
                    return Result.success(data);
                }
            }
        } else {
            data.put("id", System.currentTimeMillis());
            data.put("sort", BANNERS.size());
            BANNERS.add(data);
        }
        return Result.success(data);
    }

    /**
     * 删除Banner
     *
     * @param id Banner ID
     * @return 操作结果
     */
    @Operation(summary = "删除Banner")
    @DeleteMapping("/banner")
    public Result<Void> deleteBanner(@RequestParam Long id) {
        BANNERS.removeIf(b -> Objects.equals(b.get("id"), id));
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

    // ==================== 运营人员管理 ====================

    /**
     * 运营人员列表
     *
     * @return 人员数组
     */
    @Operation(summary = "运营人员列表")
    @GetMapping("/operators")
    public Result<List<Map<String, Object>>> getOperators() {
        return Result.success(OPERATORS);
    }

    /**
     * 创建运营人员
     *
     * @param data 包含 account/password/nickname/role
     * @return 操作结果
     */
    @Operation(summary = "创建运营人员")
    @PostMapping("/operator")
    public Result<Map<String, Object>> createOperator(@RequestBody Map<String, Object> data) {
        data.put("id", System.currentTimeMillis());
        data.put("status", 1);
        data.put("createTime", new Date());
        OPERATORS.add(data);
        return Result.success(data);
    }

    /**
     * 更新运营人员
     *
     * @param data 人员数据
     * @return 操作结果
     */
    @Operation(summary = "更新运营人员")
    @PutMapping("/operator")
    public Result<Map<String, Object>> updateOperator(@RequestBody Map<String, Object> data) {
        for (int i = 0; i < OPERATORS.size(); i++) {
            if (Objects.equals(OPERATORS.get(i).get("id"), data.get("id"))) {
                OPERATORS.set(i, data);
                return Result.success(data);
            }
        }
        return Result.success(data);
    }

    /**
     * 删除运营人员
     *
     * @param adminId 人员ID
     * @return 操作结果
     */
    @Operation(summary = "删除运营人员")
    @DeleteMapping("/operator")
    public Result<Void> deleteOperator(@RequestParam Long adminId) {
        OPERATORS.removeIf(o -> Objects.equals(o.get("id"), adminId));
        return Result.success();
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
