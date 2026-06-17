package com.minthive.ai;

import com.minthive.config.AiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AiFallback {

    @Autowired
    private AiConfig aiConfig;

    /**
     * 生成帖子文案降级
     *
     * @param keywords 关键词
     * @return 降级文案列表
     */
    public List<String> fallbackPostContent(String keywords) {
        log.warn("AI生成文案降级");
        String msg = aiConfig.getFallback().getMessage();
        return Arrays.asList(msg, msg, msg);
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
