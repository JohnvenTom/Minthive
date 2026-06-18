package com.minthive.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
     * <p>通过 SSE 逐块推送 AI 回复，前端可实现打字机效果</p>
     *
     * @param question 用户提问
     * @return SseEmitter 流式发射器
     */
    @Override
    public SseEmitter smartQaStream(String question) {
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
                callStreamingApiWithSystem(emitter, systemPrompt, question);
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
     * @throws Exception 调用失败时抛出
     */
    private void callStreamingApiWithSystem(SseEmitter emitter, String systemPrompt, String userMessage) throws Exception {
        AiConfig.Cloud cloud = aiConfig.getCloud();

        // 构建流式请求体（stream: true）
        String requestBody = objectMapper.writeValueAsString(
                new StreamChatRequest(
                        cloud.getModel(),
                        List.of(
                                new Message("system", systemPrompt),
                                new Message("user", userMessage)
                        ),
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
                    }
                }
            }
        }
        log.info("[CloudAI] 流式API调用完成");
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
     */
    private List<String> parseComments(String response) {
        String[] lines = response.split("\n");
        List<String> comments = Arrays.stream(lines)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(3)
                .toList();
        if (comments.size() >= 3) {
            return comments;
        }
        return Arrays.asList("内容很赞，学到了！", "同好一枚，期待更多分享～", "这个角度很新颖，感谢楼主。");
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
