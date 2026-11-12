package com.minthive.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 帖子文案草稿
 *
 * <p>功能描述：作为 AI 文案生成接口的返回单元，把「风格标识、风格名称、正文、话题」
 * 显式交给前端，避免前端按下标猜测风格</p>
 * <p>注意事项：style 只会是 simple / vibe / pro；content 已剥离版本前缀与外层引号；
 * topics 不带 # 前缀</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPostDraft {

    /** 风格标识：simple / vibe / pro */
    private String style;

    /** 风格名称：简约 / 氛围 / 干货 */
    private String styleName;

    /** 文案正文 */
    private String content;

    /** 话题标签，不带 # 前缀 */
    private List<String> topics;
}
