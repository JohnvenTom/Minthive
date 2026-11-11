package com.minthive.controller;

import com.minthive.ai.AiContext;
import com.minthive.ai.AiRateLimiter;
import com.minthive.ai.AiService;
import com.minthive.common.Result;
import com.minthive.common.ResultCode;
import com.minthive.entity.Circle;
import com.minthive.entity.User;
import com.minthive.security.UserContext;
import com.minthive.service.CircleService;
import com.minthive.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * AI 接口控制器(6 大 AI 能力)
 *
 * <p>功能描述：提供 AI 文案生成、润色、评论、私信代回复、内容检测、智能问答接口</p>
 * <p>注意事项：所有接口均经过 AI 限流(每用户每日 50 次)，异常自动降级</p>
 */
@Tag(name = "AI接口")
@RestController
@RequestMapping("/api/ai")
@Validated
public class AiController {

    private final AiContext aiContext;
    private final AiRateLimiter aiRateLimiter;
    private final CircleService circleService;
    private final FollowService followService;

    /**
     * 构造器注入
     */
    public AiController(AiContext aiContext, AiRateLimiter aiRateLimiter, CircleService circleService, FollowService followService) {
        this.aiContext = aiContext;
        this.aiRateLimiter = aiRateLimiter;
        this.circleService = circleService;
        this.followService = followService;
    }

    /**
     * AI 一键生成帖子文案
     *
     * @param dto 请求参数(关键词、圈子分类)
     * @return 三版风格文案
     */
    @Operation(summary = "AI生成帖子文案")
    @PostMapping("/post-content")
    public Result<List<String>> generatePostContent(@RequestBody @Valid PostContentDto dto) {
        List<String> result = withQuota(() -> aiContext.generatePostContent(dto.getKeywords(), dto.getCategory()));
        return Result.success(result);
    }

    /**
     * AI 文案润色
     *
     * @param dto 请求参数(原始文案)
     * @return 润色后文案
     */
    @Operation(summary = "AI文案润色")
    @PostMapping("/polish")
    public Result<String> polish(@RequestBody @Valid PolishDto dto) {
        String result = withQuota(() -> aiContext.polishContent(dto.getContent()));
        return Result.success(result);
    }

    /**
     * AI 智能评论生成
     *
     * @param dto 请求参数(帖子内容、圈子分类)
     * @return 3 条评论
     */
    @Operation(summary = "AI智能评论生成")
    @PostMapping("/comment")
    public Result<List<String>> generateComment(@RequestBody @Valid CommentDto dto) {
        List<String> result = withQuota(() -> aiContext.generateComment(dto.getPostContent(), dto.getCategory()));
        return Result.success(result);
    }

    /**
     * AI 私信代回复
     *
     * @param dto 请求参数(上下文、对方消息)
     * @return AI 回复
     */
    @Operation(summary = "AI私信代回复")
    @PostMapping("/message-reply")
    public Result<String> messageReply(@RequestBody @Valid MessageReplyDto dto) {
        String result = withQuota(() -> aiContext.generateMessageReply(dto.getContext(), dto.getIncomingMessage()));
        return Result.success(result);
    }

    /**
     * AI 内容检测
     *
     * @param dto 请求参数(文本/图片Base64)
     * @return 检测结果
     */
    @Operation(summary = "AI内容检测")
    @PostMapping("/detect")
    public Result<AiService.AiDetectResult> detect(@RequestBody @Valid DetectDto dto) {
        AiService.AiDetectResult result = withQuota(() -> aiContext.detectContent(dto.getText(), dto.getImageBase64()));
        if (result == null) {
            result = AiService.AiDetectResult.unavailable("审核未返回结果");
        }
        if (!result.available) {
            // 保留 data 中的旧字段，但不能以成功审核响应掩盖不可用状态。
            return Result.error(ResultCode.AI_SERVICE_ERROR,
                    "内容审核不可用：" + result.violationType, result);
        }
        return Result.success(result);
    }

    /**
     * AI 智能问答
     *
     * @param dto 请求参数(用户提问)
     * @return 回答
     */
    @Operation(summary = "AI智能问答")
    @PostMapping("/qa")
    public Result<String> qa(@RequestBody @Valid QaDto dto) {
        String result = withQuota(() -> aiContext.smartQa(dto.getQuestion()));
        return Result.success(result);
    }

    /**
     * AI 智能问答（流式输出）
     *
     * <p>通过 SSE (Server-Sent Events) 逐块推送 AI 回复，
     * 前端可实现打字机效果，用户体验更流畅</p>
     *
     * @param dto 请求参数(用户提问)
     * @return SseEmitter 流式发射器
     */
    @Operation(summary = "AI智能问答(流式)")
    @PostMapping(value = "/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter qaStream(@RequestBody @Valid QaDto dto) {
        return withQuota(() -> aiContext.smartQaStream(dto.getQuestion(), dto.getHistory()));
    }

    /**
     * AI 数据感知问答（流式输出）
     *
     * <p>自动识别用户提问中是否涉及数据查询需求（帖子详情、平台统计、热门帖子等），
     * 若有则从数据库查询真实数据，将数据拼入提示词让 LLM 总结回答。
     * 回答流式输出过程中，可能包含 {@code type: "navigation"} 事件（渲染导航卡片）
     * 和 {@code type: "meta"} 事件（标记是否使用了实时数据）。</p>
     *
     * @param dto 请求参数（用户提问 + 对话历史）
     * @return SseEmitter 流式发射器
     */
    @Operation(summary = "AI数据感知问答(流式)")
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryStream(@RequestBody @Valid QaDto dto) {
        return withQuota(() -> aiContext.queryStream(dto.getQuestion(), dto.getHistory()));
    }

    /**
     * 查询今日剩余 AI 调用次数
     *
     * @return 剩余次数
     */
    @Operation(summary = "查询今日剩余AI调用次数")
    @GetMapping("/remaining")
    public Result<Integer> remaining() {
        return Result.success(aiRateLimiter.remaining());
    }

    /**
     * AI 兴趣推荐
     *
     * <p>功能描述：基于用户兴趣推荐圈子和用户</p>
     *
     * @return 推荐结果Map，包含 circles（圈子ID列表）和 users（用户ID列表）
     */
    @Operation(summary = "AI兴趣推荐")
    @GetMapping("/recommend")
    public Result<Map<String, List<Long>>> recommend() {
        Long userId = UserContext.getUserId();
        Map<String, List<Long>> result = new HashMap<>();

        try {
            List<Circle> circles = circleService.recommendCircles();
            result.put("circles", circles.stream().map(Circle::getId).collect(Collectors.toList()));
        } catch (Exception e) {
            result.put("circles", Collections.emptyList());
        }

        try {
            List<User> users = followService.recommendUsers(userId);
            result.put("users", users.stream().map(User::getId).collect(Collectors.toList()));
        } catch (Exception e) {
            result.put("users", Collections.emptyList());
        }

        return Result.success(result);
    }

    /**
     * 带额度预占执行 AI 调用
     *
     * <p>功能描述：调用前原子预占一次每日额度，调用失败时回滚额度，
     * 避免并发请求同时通过上限校验导致超额调用</p>
     *
     * @param action 实际 AI 调用
     * @param <T>    返回值类型
     * @return AI 调用结果
     * @throws RuntimeException AI 调用抛出的异常原样向上抛出
     * @note 额度预占依赖 Redis；Redis 不可用时按现有降级策略放行
     */
    private <T> T withQuota(Supplier<T> action) {
        aiRateLimiter.reserve();
        try {
            return action.get();
        } catch (RuntimeException e) {
            aiRateLimiter.release();
            throw e;
        }
    }

    /**
     * 文案生成请求 DTO
     */
    @Data
    public static class PostContentDto {
        @Size(max = 100, message = "关键词不能超过100字")
        private String keywords;
        @Size(max = 50, message = "分类不能超过50字")
        private String category;
    }

    /**
     * 文案润色请求 DTO
     */
    @Data
    public static class PolishDto {
        @Size(max = 5000, message = "文案不能超过5000字")
        private String content;
    }

    /**
     * 评论生成请求 DTO
     */
    @Data
    public static class CommentDto {
        @Size(max = 5000, message = "帖子内容不能超过5000字")
        private String postContent;
        @Size(max = 50, message = "分类不能超过50字")
        private String category;
    }

    /**
     * 私信代回复请求 DTO
     */
    @Data
    public static class MessageReplyDto {
        @Size(max = 5000, message = "上下文不能超过5000字")
        private String context;
        @Size(max = 2000, message = "对方消息不能超过2000字")
        private String incomingMessage;
    }

    /**
     * 内容检测请求 DTO
     */
    @Data
    public static class DetectDto {
        @Size(max = 5000, message = "文本不能超过5000字")
        private String text;
        @Size(max = 8000000, message = "图片过大，无法审核")
        private String imageBase64;
    }

    /**
     * 智能问答请求 DTO
     */
    @Data
    public static class QaDto {
        @Size(max = 2000, message = "问题不能超过2000字")
        private String question;
        /** 对话历史（偶数索引为 user，奇数索引为 ai，用于上下文连贯） */
        @Size(max = 20, message = "对话历史过多")
        private List<String> history;
    }
}
