package com.minthive.ai;

import java.util.List;

/**
 * AI 服务接口
 *
 * <p>功能描述：定义 6 大 AI 能力接口签名，由 CloudAiServiceImpl / LocalAiServiceImpl 实现</p>
 * <p>注意事项：所有方法应支持降级返回，避免阻塞业务</p>
 */
public interface AiService {

    /**
     * AI 一键生成帖子文案
     *
     * @param keywords 关键词
     * @param category 圈子分类
     * @return 三版风格文案(简约版/氛围感版/干货版)
     */
    List<String> generatePostContent(String keywords, String category);

    /**
     * AI 文案润色/纠错
     *
     * @param rawContent 原始文案
     * @return 润色后文案(含话题标签)
     */
    String polishContent(String rawContent);

    /**
     * AI 智能评论生成
     *
     * @param postContent 帖子内容
     * @param category    圈子分类
     * @return 3 条不同风格评论
     */
    List<String> generateComment(String postContent, String category);

    /**
     * AI 私信代回复
     *
     * @param context 聊天上下文
     * @param incomingMessage 对方消息内容
     * @return AI 回复文案
     */
    String generateMessageReply(String context, String incomingMessage);

    /**
     * AI 内容检测
     *
     * @param text     文本内容(可空)
     * @param imageBase64 图片Base64(可空)
     * @return 检测结果: 是否违规、违规类型、风险等级
     */
    AiDetectResult detectContent(String text, String imageBase64);

    /**
     * AI 智能问答
     *
     * @param question 用户提问
     * @return 平台规则标准化回答
     */
    String smartQa(String question);

    /**
     * AI 内容检测结果封装
     */
    class AiDetectResult {
        /** 是否违规 */
        public boolean violation;
        /** 违规类型 */
        public String violationType;
        /** 风险等级: low/medium/high */
        public String riskLevel;

        public AiDetectResult(boolean violation, String violationType, String riskLevel) {
            this.violation = violation;
            this.violationType = violationType;
            this.riskLevel = riskLevel;
        }
    }
}
