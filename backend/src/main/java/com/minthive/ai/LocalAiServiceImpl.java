package com.minthive.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 本地私有化 AI 服务实现
 *
 * <p>功能描述：基于本地 Ollama + Qwen3-7B 模型实现 6 大 AI 能力，全程内网调用</p>
 * <p>注意事项：实际大模型调用此处使用占位实现，生产环境需对接 Ollama HTTP API</p>
 */
@Slf4j
@Service("localAiService")
@RequiredArgsConstructor
public class LocalAiServiceImpl implements AiService {

    private final AiFallback aiFallback;

    /**
     * AI 一键生成帖子文案(本地)
     *
     * @param keywords 关键词
     * @param category 圈子分类
     * @return 三版风格文案
     */
    @Override
    public List<String> generatePostContent(String keywords, String category) {
        try {
            // TODO: 调用 Ollama HTTP API: POST http://localhost:11434/api/generate
            log.info("[LocalAI] 生成帖子文案: keywords={}, category={}", keywords, category);
            return Arrays.asList(
                    "【简约版】" + keywords + "，简单记录。",
                    "【氛围感版】" + category + "时光，" + keywords + "最动人。",
                    "【干货版】" + keywords + "实用指南，建议收藏。"
            );
        } catch (Exception e) {
            log.error("[LocalAI] 生成文案异常: ", e);
            return aiFallback.fallbackPostContent(keywords);
        }
    }

    /**
     * AI 文案润色(本地)
     *
     * @param rawContent 原始文案
     * @return 润色后文案
     */
    @Override
    public String polishContent(String rawContent) {
        try {
            log.info("[LocalAI] 文案润色");
            return rawContent + " #兴趣 #分享";
        } catch (Exception e) {
            log.error("[LocalAI] 文案润色异常: ", e);
            return aiFallback.fallbackPolish(rawContent);
        }
    }

    /**
     * AI 智能评论生成(本地)
     *
     * @param postContent 帖子内容
     * @param category    圈子分类
     * @return 3 条评论
     */
    @Override
    public List<String> generateComment(String postContent, String category) {
        try {
            log.info("[LocalAI] 生成评论");
            return Arrays.asList("支持一下！", "同感，期待更多。", "感谢分享，已收藏。");
        } catch (Exception e) {
            log.error("[LocalAI] 生成评论异常: ", e);
            return aiFallback.fallbackComment();
        }
    }

    /**
     * AI 私信代回复(本地)
     *
     * @param context         聊天上下文
     * @param incomingMessage 对方消息
     * @return AI 回复
     */
    @Override
    public String generateMessageReply(String context, String incomingMessage) {
        try {
            log.info("[LocalAI] 私信代回复");
            return "我现在不方便回复，稍后联系～";
        } catch (Exception e) {
            log.error("[LocalAI] 私信代回复异常: ", e);
            return aiFallback.fallbackMessageReply();
        }
    }

    /**
     * AI 内容检测(本地)
     *
     * @param text        文本
     * @param imageBase64 图片Base64
     * @return 检测结果
     */
    @Override
    public AiDetectResult detectContent(String text, String imageBase64) {
        try {
            log.info("[LocalAI] 内容检测");
            return new AiDetectResult(false, "无", "low");
        } catch (Exception e) {
            log.error("[LocalAI] 内容检测异常: ", e);
            return new AiDetectResult(false, "检测异常", "low");
        }
    }

    /**
     * AI 智能问答(本地)
     *
     * @param question 用户提问
     * @return 回答
     */
    @Override
    public String smartQa(String question) {
        try {
            log.info("[LocalAI] 智能问答: {}", question);
            return "您好，关于「" + question + "」，请参考平台规则说明。";
        } catch (Exception e) {
            log.error("[LocalAI] 智能问答异常: ", e);
            return aiFallback.fallbackQa();
        }
    }

    /**
     * AI 智能问答(流式输出，本地模式)
     *
     * <p>本地模式不支持真正的流式输出，模拟一次性返回</p>
     *
     * @param question 用户提问
     * @param history  对话历史（本地模式暂不使用）
     * @return SseEmitter 流式发射器（一次性发送完整回答）
     */
    @Override
    public SseEmitter smartQaStream(String question, List<String> history) {
        SseEmitter emitter = new SseEmitter(10_000L);
        CompletableFuture.runAsync(() -> {
            try {
                String answer = smartQa(question);
                emitter.send(SseEmitter.event().name("message").data(answer, org.springframework.http.MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                log.error("[LocalAI] 智能问答流式异常: ", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(aiFallback.fallbackQa(), org.springframework.http.MediaType.APPLICATION_JSON));
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
            }
        });
        return emitter;
    }
}
