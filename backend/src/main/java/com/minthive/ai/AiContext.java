package com.minthive.ai;

import com.minthive.config.AiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 上下文(策略选择)
 *
 * <p>功能描述：根据配置 ai.mode 动态选择 CloudAiServiceImpl 或 LocalAiServiceImpl</p>
 * <p>注意事项：切换模式无需改动代码，仅需修改配置</p>
 */
@Component
public class AiContext {

    @Autowired
    private AiConfig aiConfig;

    @Autowired
    @Qualifier("cloudAiService")
    private AiService cloudAiService;

    @Autowired
    @Qualifier("localAiService")
    private AiService localAiService;

    /**
     * 获取当前生效的 AI 服务实现
     *
     * @return AiService 实例
     */
    public AiService getAiService() {
        String mode = aiConfig.getMode();
        if ("cloud".equalsIgnoreCase(mode)) {
            return cloudAiService;
        }
        return localAiService;
    }

    /**
     * 代理调用：生成帖子文案
     *
     * @param keywords 关键词
     * @param category 圈子分类
     * @return 三版文案
     */
    public List<String> generatePostContent(String keywords, String category) {
        return getAiService().generatePostContent(keywords, category);
    }

    /**
     * 代理调用：文案润色
     *
     * @param rawContent 原始文案
     * @return 润色后文案
     */
    public String polishContent(String rawContent) {
        return getAiService().polishContent(rawContent);
    }

    /**
     * 代理调用：生成评论
     *
     * @param postContent 帖子内容
     * @param category    圈子分类
     * @return 3 条评论
     */
    public List<String> generateComment(String postContent, String category) {
        return getAiService().generateComment(postContent, category);
    }

    /**
     * 代理调用：私信代回复
     *
     * @param context         上下文
     * @param incomingMessage 对方消息
     * @return AI 回复
     */
    public String generateMessageReply(String context, String incomingMessage) {
        return getAiService().generateMessageReply(context, incomingMessage);
    }

    /**
     * 代理调用：内容检测
     *
     * @param text        文本
     * @param imageBase64 图片Base64
     * @return 检测结果
     */
    public AiService.AiDetectResult detectContent(String text, String imageBase64) {
        return getAiService().detectContent(text, imageBase64);
    }

    /**
     * 代理调用：智能问答
     *
     * @param question 用户提问
     * @return 回答
     */
    public String smartQa(String question) {
        return getAiService().smartQa(question);
    }
}
