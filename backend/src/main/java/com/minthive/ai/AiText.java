package com.minthive.ai;

/**
 * AI 返回文本清洗工具
 *
 * <p>功能描述：模型常把结果包在 ```json ... ``` 代码块中返回，
 * 这里统一剥离代码块标记，供 JSON 解析类调用</p>
 * <p>注意事项：只剥离标记本身，不改写内容；剥离结果仍需调用方做格式校验</p>
 */
public final class AiText {

    /**
     * 私有构造，禁止实例化工具类
     */
    private AiText() {
    }

    /**
     * 去除 Markdown 代码块包装
     *
     * @param response 模型原始输出，可为空
     * @return 去除代码块标记并裁剪空白后的文本；非代码块输出原样返回
     */
    public static String stripCodeFence(String response) {
        if (response == null) {
            return "";
        }
        String text = response.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstLineEnd = text.indexOf('\n');
        if (firstLineEnd > 0) {
            text = text.substring(firstLineEnd + 1);
        }
        int closing = text.lastIndexOf("```");
        if (closing >= 0) {
            text = text.substring(0, closing);
        }
        return text.trim();
    }
}
