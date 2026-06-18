package com.minthive.controller;

import com.minthive.ai.AiContext;
import com.minthive.ai.AiRateLimiter;
import com.minthive.ai.AiService;
import com.minthive.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * AI 接口控制器(6 大 AI 能力)
 *
 * <p>功能描述：提供 AI 文案生成、润色、评论、私信代回复、内容检测、智能问答接口</p>
 * <p>注意事项：所有接口均经过 AI 限流(每用户每日 50 次)，异常自动降级</p>
 */
@Tag(name = "AI接口")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiContext aiContext;
    private final AiRateLimiter aiRateLimiter;

    /**
     * 构造器注入 AiContext 和 AiRateLimiter
     *
     * @param aiContext     AI上下文
     * @param aiRateLimiter AI限流器
     */
    public AiController(AiContext aiContext, AiRateLimiter aiRateLimiter) {
        this.aiContext = aiContext;
        this.aiRateLimiter = aiRateLimiter;
    }

    /**
     * AI 一键生成帖子文案
     *
     * @param dto 请求参数(关键词、圈子分类)
     * @return 三版风格文案
     */
    @Operation(summary = "AI生成帖子文案")
    @PostMapping("/post-content")
    public Result<List<String>> generatePostContent(@RequestBody PostContentDto dto) {
        aiRateLimiter.checkLimit();
        List<String> result = aiContext.generatePostContent(dto.getKeywords(), dto.getCategory());
        aiRateLimiter.increment();
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
    public Result<String> polish(@RequestBody PolishDto dto) {
        aiRateLimiter.checkLimit();
        String result = aiContext.polishContent(dto.getContent());
        aiRateLimiter.increment();
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
    public Result<List<String>> generateComment(@RequestBody CommentDto dto) {
        aiRateLimiter.checkLimit();
        List<String> result = aiContext.generateComment(dto.getPostContent(), dto.getCategory());
        aiRateLimiter.increment();
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
    public Result<String> messageReply(@RequestBody MessageReplyDto dto) {
        aiRateLimiter.checkLimit();
        String result = aiContext.generateMessageReply(dto.getContext(), dto.getIncomingMessage());
        aiRateLimiter.increment();
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
    public Result<AiService.AiDetectResult> detect(@RequestBody DetectDto dto) {
        aiRateLimiter.checkLimit();
        AiService.AiDetectResult result = aiContext.detectContent(dto.getText(), dto.getImageBase64());
        aiRateLimiter.increment();
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
    public Result<String> qa(@RequestBody QaDto dto) {
        aiRateLimiter.checkLimit();
        String result = aiContext.smartQa(dto.getQuestion());
        aiRateLimiter.increment();
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
    public SseEmitter qaStream(@RequestBody QaDto dto) {
        aiRateLimiter.checkLimit();
        SseEmitter emitter = aiContext.smartQaStream(dto.getQuestion());
        aiRateLimiter.increment();
        return emitter;
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
     * <p>功能描述：基于用户兴趣推荐圈子和用户，简化实现为随机推荐</p>
     * <p>注意事项：当前为随机推荐3个圈子ID和3个用户ID，后续可接入真实AI推荐算法</p>
     *
     * @return 推荐结果Map，包含 circles（圈子ID列表）和 users（用户ID列表）
     */
    @Operation(summary = "AI兴趣推荐")
    @GetMapping("/recommend")
    public Result<Map<String, List<Integer>>> recommend() {
        Random random = new Random();
        Map<String, List<Integer>> result = new HashMap<>();
        // 随机推荐3个圈子ID
        result.put("circles", Arrays.asList(
                random.nextInt(100) + 1,
                random.nextInt(100) + 1,
                random.nextInt(100) + 1
        ));
        // 随机推荐3个用户ID
        result.put("users", Arrays.asList(
                random.nextInt(1000) + 1,
                random.nextInt(1000) + 1,
                random.nextInt(1000) + 1
        ));
        return Result.success(result);
    }

    /**
     * 文案生成请求 DTO
     */
    @Data
    public static class PostContentDto {
        private String keywords;
        private String category;
    }

    /**
     * 文案润色请求 DTO
     */
    @Data
    public static class PolishDto {
        private String content;
    }

    /**
     * 评论生成请求 DTO
     */
    @Data
    public static class CommentDto {
        private String postContent;
        private String category;
    }

    /**
     * 私信代回复请求 DTO
     */
    @Data
    public static class MessageReplyDto {
        private String context;
        private String incomingMessage;
    }

    /**
     * 内容检测请求 DTO
     */
    @Data
    public static class DetectDto {
        private String text;
        private String imageBase64;
    }

    /**
     * 智能问答请求 DTO
     */
    @Data
    public static class QaDto {
        private String question;
    }
}
