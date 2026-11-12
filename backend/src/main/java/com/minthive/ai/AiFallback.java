package com.minthive.ai;

import com.minthive.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * AI 降级处理器
 *
 * <p>功能描述：当 AI 服务异常时返回固定提示文案，不阻塞业务流程</p>
 * <p>注意事项：降级文案可通过 ai.fallback.message 配置覆盖</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiFallback {

    private final AiConfig aiConfig;

    /**
     * 生成帖子文案降级
     *
     * <p>返回三个固定风格的空壳草稿，正文为降级提示语，保证前端仍能渲染三张卡片</p>
     *
     * @param keywords 关键词（当前未参与降级文案，保留参数便于后续差异化）
     * @return 降级文案列表
     */
    public List<AiPostDraft> fallbackPostContent(String keywords) {
        log.warn("AI生成文案降级");
        String msg = aiConfig.getFallback().getMessage();
        return List.of(
                AiPostDraft.builder().style("simple").styleName("简约").content(msg).topics(List.of()).build(),
                AiPostDraft.builder().style("vibe").styleName("氛围").content(msg).topics(List.of()).build(),
                AiPostDraft.builder().style("pro").styleName("干货").content(msg).topics(List.of()).build()
        );
    }

    /**
     * 文案润色降级
     *
     * @param rawContent 原始文案
     * @return 降级文案
     */
    public String fallbackPolish(String rawContent) {
        log.warn("AI文案润色降级");
        return rawContent == null ? aiConfig.getFallback().getMessage() : rawContent;
    }

    /**
     * 评论生成降级
     *
     * @return 降级评论列表
     */
    public List<String> fallbackComment() {
        log.warn("AI评论生成降级");
        String msg = aiConfig.getFallback().getMessage();
        return Arrays.asList(msg, msg, msg);
    }

    /**
     * 私信代回复降级
     *
     * @return 降级回复
     */
    public String fallbackMessageReply() {
        log.warn("AI私信代回复降级");
        return aiConfig.getFallback().getMessage();
    }

    /**
     * 智能问答降级
     *
     * @return 降级回答
     */
    public String fallbackQa() {
        log.warn("AI智能问答降级");
        return aiConfig.getFallback().getMessage();
    }
}
