package com.minthive.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 公有云 AI 服务实现
 *
 * <p>功能描述：基于 OpenAI 兼容 API 调用公有云大模型，实现 6 大 AI 能力</p>
 * <p>支持所有兼容 OpenAI Chat Completions 接口的服务（DeepSeek、Agnes、通义千问等）</p>
 * <p>注意事项：通过 application-dev.yml 的 ai.cloud.* 配置连接目标服务</p>
 */
@Slf4j
@Service("cloudAiService")
@RequiredArgsConstructor
public class CloudAiServiceImpl implements AiService {

    private final AiConfig aiConfig;
    private final AiFallback aiFallback;
    private final ObjectMapper objectMapper;

    @Autowired
    private AiToolService aiToolService;

    /** HTTP 客户端（复用连接池） */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * AI 一键生成帖子文案(公有云)
     *
     * @param keywords 关键词
     * @param category 圈子分类
     * @return 三版风格文案（简约/氛围感/干货）
     */
    @Override
    public List<String> generatePostContent(String keywords, String category) {
        try {
            String prompt = buildPostPrompt(keywords, category);
            String response = callChatApi(prompt);
            return parseThreeVersions(response, keywords, category);
        } catch (Exception e) {
            log.error("[CloudAI] 生成文案异常: ", e);
            return aiFallback.fallbackPostContent(keywords);
        }
    }

    /**
     * AI 文案润色(公有云)
     *
     * @param rawContent 原始文案
     * @return 润色后文案
     */
    @Override
    public String polishContent(String rawContent) {
        try {
            String prompt = "请对以下社交平台文案进行润色，保持原意，使其更生动、更有吸引力：\n" + rawContent;
            return callChatApi(prompt);
        } catch (Exception e) {
            log.error("[CloudAI] 文案润色异常: ", e);
            return aiFallback.fallbackPolish(rawContent);
        }
    }

    /**
     * AI 智能评论生成(公有云)
     *
     * @param postContent 帖子内容
     * @param category    圈子分类
     * @return 3 条评论
     */
    @Override
    public List<String> generateComment(String postContent, String category) {
        try {
            String prompt = "请针对以下「" + category + "」圈子的帖子内容，生成3条不同风格的友好评论（每条不超过30字）：\n" + postContent;
            String response = callChatApi(prompt);
            log.info("[CloudAI] 评论生成原始回复: {}", response);
            return parseComments(response);
        } catch (Exception e) {
            log.error("[CloudAI] 生成评论异常: ", e);
            return aiFallback.fallbackComment();
        }
    }

    /**
     * AI 私信代回复(公有云)
     *
     * @param context         聊天上下文
     * @param incomingMessage 对方消息
     * @return AI 回复
     */
    @Override
    public String generateMessageReply(String context, String incomingMessage) {
        try {
            String prompt = "你是MintHive兴趣社交平台的用户代回复助手。根据聊天上下文和对方最新消息，生成一条自然、友好的中文回复（20字以内）。\n"
                    + "聊天上下文：" + (context != null ? context : "无") + "\n"
                    + "对方消息：" + incomingMessage;
            return callChatApi(prompt);
        } catch (Exception e) {
            log.error("[CloudAI] 私信代回复异常: ", e);
            return aiFallback.fallbackMessageReply();
        }
    }

    /**
     * AI 内容检测(公有云)
     *
     * <p>检测文本是否包含违规内容</p>
     *
     * @param text        文本
     * @param imageBase64 图片Base64（可选）
     * @return 检测结果
     */
    @Override
    public AiDetectResult detectContent(String text, String imageBase64) {
        try {
            String prompt = "请判断以下文本是否包含违规内容（广告、暴力、色情、政治敏感等）。只回答：safe 或 unsafe。\n文本：" + text;
            String result = callChatApi(prompt);
            boolean unsafe = result.toLowerCase().contains("unsafe");
            return new AiDetectResult(unsafe, unsafe ? "疑似违规内容" : "无", unsafe ? "high" : "low");
        } catch (Exception e) {
            log.error("[CloudAI] 内容检测异常: ", e);
            return new AiDetectResult(false, "检测异常", "low");
        }
    }

    /**
     * AI 智能问答(公有云)
     *
     * <p>调用大模型进行通用问答，用于 MintHive 平台的智能助手功能</p>
     *
     * @param question 用户提问
     * @return 大模型回答
     */
    @Override
    public String smartQa(String question) {
        try {
            String systemPrompt = "你是MintHive兴趣社交平台的智能助手（代号MinthiveAI），一个面向户外运动和兴趣爱好者的社区平台。"
                    + "你的职责是帮助用户了解平台功能、提供户外活动建议、兴趣圈子推荐等。"
                    + "回答要简洁实用（100字以内），语气友好自然。如果问题与平台无关，也尽量结合兴趣社交角度给出建议。";
            return callChatApiWithSystem(systemPrompt, question);
        } catch (Exception e) {
            log.error("[CloudAI] 智能问答异常: ", e);
            return aiFallback.fallbackQa();
        }
    }

    /**
     * AI 智能问答(流式输出)
     *
     * <p>通过 SSE 逐块推送 AI 回复，前端可实现打字机效果。
     * 支持传入对话历史以保持上下文连贯。</p>
     *
     * @param question 用户提问
     * @param history  对话历史（偶数索引为 user 消息，奇数索引为 ai 回复），可为 null
     * @return SseEmitter 流式发射器
     */
    @Override
    public SseEmitter smartQaStream(String question, List<String> history) {
        // SSE 超时时间 120 秒（流式传输需要足够时间）
        SseEmitter emitter = new SseEmitter(120_000L);

        // 注册错误回调，防止异常泄漏到 GlobalExceptionHandler 导致 text/event-stream 序列化失败
        emitter.onError((e) -> log.warn("[CloudAI] SSE 连接异常: {}", e.getMessage()));

        // 异步执行，避免阻塞 Tomcat 线程池
        CompletableFuture.runAsync(() -> {
            try {
                String systemPrompt = "你是MintHive兴趣社交平台的智能助手（代号MinthiveAI），一个面向户外运动和兴趣爱好者的社区平台。"
                        + "你的职责是帮助用户了解平台功能、提供户外活动建议、兴趣圈子推荐等。"
                        + "回答要简洁实用（100字以内），语气友好自然。如果问题与平台无关，也尽量结合兴趣社交角度给出建议。";
                callStreamingApiWithSystem(emitter, systemPrompt, question, history);
                emitter.complete();
            } catch (Exception e) {
                log.error("[CloudAI] 智能问答流式异常: ", e);
                try {
                    // 发送降级文案后正常完成（不调用 completeWithError，避免异常泄漏到 Servlet 层）
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(aiFallback.fallbackQa(), MediaType.APPLICATION_JSON));
                    emitter.complete();
                } catch (Exception ignored) {
                    // 发送失败事件异常时忽略
                }
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter queryStream(String question, List<String> history) {
        SseEmitter emitter = new SseEmitter(120_000L);
        emitter.onError((e) -> log.warn("[CloudAI] queryStream SSE 连接异常: {}", e.getMessage()));

        CompletableFuture.runAsync(() -> {
            try {
                // 1. 解析意图
                IntentParser intentParser = new IntentParser();
                IntentParser.ParsedIntent intent = intentParser.parse(question);

                ToolResult toolResult = null;
                boolean hasRealData = false;

                // 2. 如果有匹配的数据查询意图，执行工具
                if (intent.getType() != IntentParser.IntentType.NONE) {
                    toolResult = aiToolService.execute(intent);
                    hasRealData = toolResult != null && toolResult.isSuccess();
                }

                // 3. 构建 system prompt（含工具数据上下文）
                String systemPrompt = buildQuerySystemPrompt(toolResult);

                // 4. 流式调用 LLM
                callStreamingApiWithSystem(emitter, systemPrompt, question, history);

                // 5. 发送导航建议（如果有）
                if (hasRealData && toolResult.getNavigation() != null && !toolResult.getNavigation().isEmpty()) {
                    String navJson = objectMapper.writeValueAsString(Map.of(
                            "type", "navigation",
                            "items", toolResult.getNavigation()
                    ));
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(navJson, MediaType.APPLICATION_JSON));
                    flushSseEmitter();
                }

                // 6. 发送元数据（标记是否使用了实时数据）
                String metaJson = objectMapper.writeValueAsString(Map.of(
                        "type", "meta",
                        "hasRealData", hasRealData
                ));
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(metaJson, MediaType.APPLICATION_JSON));
                flushSseEmitter();

                emitter.complete();
            } catch (Exception e) {
                log.error("[CloudAI] queryStream 异常: ", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(aiFallback.fallbackQa(), MediaType.APPLICATION_JSON));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    private String buildQuerySystemPrompt(ToolResult toolResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是MintHive兴趣社交平台的智能助手（代号MinthiveAI），一个面向户外运动和兴趣爱好者的社区平台。");
        sb.append("你的职责是帮助用户了解平台功能、提供户外活动建议、兴趣圈子推荐等。");
        sb.append("回答要简洁实用（100字以内），语气友好自然。");

        if (toolResult != null && toolResult.isSuccess()) {
            sb.append("\n\n【📊 实时数据 - ").append(toolResult.getToolDisplayName()).append("】\n");
            sb.append(toolResult.getDataSummary());
            sb.append("\n\n请基于以上真实数据回答用户的问题。如果数据中包含帖子或圈子，可以在描述中自然提及。");
        }

        return sb.toString();
    }

    // ==================== 私有方法 ====================

    /**
     * 调用 OpenAI 兼容的 Chat Completions 接口
     *
     * @param userMessage 用户消息
     * @return 模型回复文本
     * @throws Exception 调用失败时抛出
     */
    private String callChatApi(String userMessage) throws Exception {
        return callChatApiWithSystem("你是一个有帮助的AI助手。", userMessage);
    }

    /**
     * 带系统提示词调用 OpenAI 兼容接口
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return 模型回复文本
     * @throws Exception 调用失败时抛出
     */
    private String callChatApiWithSystem(String systemPrompt, String userMessage) throws Exception {
        AiConfig.Cloud cloud = aiConfig.getCloud();

        // 构建请求体（OpenAI Chat Completions 格式）
        String requestBody = objectMapper.writeValueAsString(
                new ChatRequest(
                        cloud.getModel(),
                        List.of(
                                new Message("system", systemPrompt),
                                new Message("user", userMessage)
                        ),
                        0.7,
                        1024
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cloud.getBaseUrl() + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + cloud.getApiKey())
                .timeout(Duration.ofMillis(cloud.getTimeout()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("[CloudAI] API 调用失败 status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("AI API 返回错误: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").get(0).path("message").path("content").asText("");
        log.info("[CloudAI] API 调用成功, 回复长度={}", content.length());
        return content;
    }

    /**
     * 带系统提示词的流式调用 OpenAI 兼容接口
     *
     * <p>请求体中设置 stream=true，响应为 SSE 格式（data: {...}\n\n），
     * 逐行解析 choices[0].delta.content 并通过 SseEmitter 推送给前端</p>
     *
     * @param emitter      SSE 发射器
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @param history      对话历史（偶数索引为 user 消息，奇数索引为 ai 回复），可为 null
     * @throws Exception 调用失败时抛出
     */
    private void callStreamingApiWithSystem(SseEmitter emitter, String systemPrompt, String userMessage, List<String> history) throws Exception {
        AiConfig.Cloud cloud = aiConfig.getCloud();

        // 构建消息列表：system + 历史对话 + 当前问题
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));

        // 将历史消息按 user/ai 角色交替注入上下文
        if (history != null && !history.isEmpty()) {
            for (int i = 0; i < history.size(); i++) {
                String role = (i % 2 == 0) ? "user" : "assistant";
                messages.add(new Message(role, history.get(i)));
            }
        }
        // 当前用户问题
        messages.add(new Message("user", userMessage));

        // 构建流式请求体（stream: true）
        String requestBody = objectMapper.writeValueAsString(
                new StreamChatRequest(
                        cloud.getModel(),
                        messages,
                        0.7,
                        1024,
                        true
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cloud.getBaseUrl() + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + cloud.getApiKey())
                // 流式请求使用更长超时（120秒），连接需保持到AI回复完毕
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            // 流式响应错误时读取错误信息
            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            log.error("[CloudAI] 流式API调用失败 status={}, body={}", response.statusCode(), errorBody);
            throw new RuntimeException("AI API 返回错误: " + response.statusCode());
        }

        // 逐行读取 SSE 响应流并推送给前端
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // SSE 格式：data: {...} 或 data: [DONE]
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    // 流结束标记
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    // 解析 chunk 并提取 delta content
                    JsonNode chunk = objectMapper.readTree(data);
                    JsonNode delta = chunk.path("choices")
                            .path(0)
                            .path("delta")
                            .path("content");
                    if (!delta.isMissingNode() && !delta.asText().isEmpty()) {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(delta.asText(), MediaType.APPLICATION_JSON));
                        // 手动刷新输出缓冲区，确保每个 chunk 立即推送到浏览器
                        // 否则 Tomcat 会缓冲数据直到连接关闭才一次性发出
                        flushSseEmitter();
                    }
                }
            }
        }
        log.info("[CloudAI] 流式API调用完成");
    }

    /**
     * 手动刷新 SseEmitter 输出缓冲区
     *
     * <p>功能描述：通过获取底层 HttpServletResponse 的 OutputStream 并 flush，
     * 强制将 SSE 数据立即推送到浏览器，避免 Tomcat 缓冲导致流式输出变成一次性输出</p>
     * <p>注意事项：仅适用于 Servlet 容器环境；非 Servlet 环境下静默跳过</p>
     */
    private void flushSseEmitter() {
        try {
            var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletResponse response = requestAttributes.getResponse();
                if (response != null) {
                    response.flushBuffer();
                }
            }
        } catch (Exception e) {
            // flush 失败不影响主流程（连接可能已关闭）
            log.debug("[CloudAI] SSE flush 缓冲区时异常（可忽略）: {}", e.getMessage());
        }
    }

    /**
     * 构建帖子文案生成的提示词
     */
    private String buildPostPrompt(String keywords, String category) {
        return "请为「" + category + "」圈子生成3个版本的社交媒体帖子文案，关键词：「" + keywords + "」。"
                + "格式要求：每个版本用【版本名】开头，之间用 ||| 分隔。"
                + "版本1【简约风】：简洁有力，1-2句话；"
                + "版本2【氛围感】：文艺有画面感，2-3句话；"
                + "版本3【干货型】：实用有价值，带具体建议，2-3句话。";
    }

    /**
     * 解析三版本文案返回值
     */
    private List<String> parseThreeVersions(String response, String keywords, String category) {
        if (response.contains("|||")) {
            String[] parts = response.split("\\|\\|\\|");
            return Arrays.asList(parts);
        }
        // 如果模型没有按格式返回，降级处理
        return Arrays.asList(
                "【简约版】" + keywords + "，记录此刻心情。",
                "【氛围感版】在" + category + "的世界里，" + keywords + "是独一无二的瞬间。",
                "【干货版】关于" + keywords + "，分享几点实用经验与心得。"
        );
    }

    /**
     * 解析评论返回值
     *
     * <p>兼容多种 AI 返回格式：换行分隔、编号列表（1. xxx）、中文序号等</p>
     */
    private List<String> parseComments(String response) {
        if (response == null || response.isBlank()) {
            return aiFallback.fallbackComment();
        }

        // 先尝试按换行分割
        String[] lines = response.split("\n");
        List<String> comments = new java.util.ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // 去除编号前缀：1. / 1、/ (1) / 【1】 等
            String cleaned = trimmed
                    .replaceAll("^(\\d+[.、)）]\\s*)", "")
                    .replaceAll("^(【\\d+】\\s*)", "")
                    .replaceAll("^[-*•]\\s*", "");

            if (!cleaned.isEmpty()) {
                comments.add(cleaned);
            }
        }

        // 如果按换行分割不足3条，尝试用其他分隔符
        if (comments.size() < 3) {
            // 尝试按 ||| 分割
            if (response.contains("|||")) {
                comments = Arrays.stream(response.split("\\|\\|\\|"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .limit(3)
                        .toList();
            }
        }

        log.info("[CloudAI] 解析出 {} 条评论: {}", comments.size(), comments);

        if (comments.size() >= 3) {
            return comments.subList(0, 3);
        }
        // 解析出的条数不足但有多于0条，补齐
        if (!comments.isEmpty()) {
            while (comments.size() < 3) {
                comments.add(aiFallback.fallbackComment().get(comments.size()));
            }
            return comments;
        }
        return aiFallback.fallbackComment();
    }

    // ==================== 内部数据类 ====================

    /**
     * OpenAI Chat Completions 请求体（同步）
     */
    record ChatRequest(String model, List<Message> messages, double temperature, int maxTokens) {}

    /**
     * OpenAI Chat Completions 请求体（流式）
     *
     * @param model      模型名称
     * @param messages   消息列表
     * @param temperature 温度参数
     * @param maxTokens  最大 token 数
     * @param stream     是否流式输出
     */
    record StreamChatRequest(String model, List<Message> messages, double temperature, int maxTokens, boolean stream) {}

    /**
     * OpenAI 消息体
     */
    record Message(String role, String content) {}
}
