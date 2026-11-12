package com.minthive.ai;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 帖子文案解析器
 *
 * <p>功能描述：把模型返回的文案原文统一解析为结构化草稿。解析顺序为
 * JSON 数组 → 【版本名】标签分段 → ||| 或空行分段，兼容云端与本地模型的不同输出习惯</p>
 * <p>注意事项：</p>
 * <ul>
 *   <li>最多返回 3 条，不足 3 条时原样返回，由调用方决定是否降级，不硬凑占位文案；</li>
 *   <li>正文统一剥离「版本N」「【xx】」前缀与外层引号，保留段落换行；</li>
 *   <li>风格标识只取 simple / vibe / pro，缺失时按出现顺序补齐。</li>
 * </ul>
 */
public final class PostDraftParser {

    /** 风格标识，按版本顺序对应 */
    private static final String[] STYLES = {"simple", "vibe", "pro"};

    /** 风格名称，与 {@link #STYLES} 一一对应 */
    private static final String[] STYLE_NAMES = {"简约", "氛围", "干货"};

    /** 版本标签：【简约风】【版本2】这类不超过 12 字的短标签 */
    private static final Pattern LABEL = Pattern.compile("【([^】]{1,12})】\\s*[:：]?");

    /** 行首版本标签，用于从正文中剥离 */
    private static final Pattern LEADING_LABEL = Pattern.compile("^\\s*【[^】]{1,12}】\\s*[:：]?\\s*");

    /** 正文中的话题标签：#美食 / ＃美食，遇到标点或空白结束 */
    private static final Pattern HASHTAG = Pattern.compile("[#＃]([^#＃\\s，。,.!！?？;；:：、()（）]{1,20})");

    /** JSON 解析器：容忍模型常见的单引号与字符串内未转义换行 */
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    /**
     * 私有构造，禁止实例化工具类
     */
    private PostDraftParser() {
    }

    /**
     * 解析模型返回的文案
     *
     * @param raw 模型原始输出，可为空
     * @return 结构化草稿列表，最多 3 条；无法识别时返回空列表
     */
    public static List<AiPostDraft> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String text = AiText.stripCodeFence(raw).trim();
        List<AiPostDraft> drafts = parseJson(text);
        if (drafts.isEmpty()) {
            drafts = parseLabels(text);
        }
        if (drafts.isEmpty()) {
            drafts = parseSeparators(text);
        }
        for (int i = 0; i < drafts.size(); i++) {
            applyStyle(drafts.get(i), i);
            extractHashtags(drafts.get(i));
        }
        return drafts;
    }

    /**
     * 按 JSON 数组解析
     *
     * @param text 已去除代码块的文本
     * @return 草稿列表，非 JSON 或解析失败时返回空列表
     */
    private static List<AiPostDraft> parseJson(String text) {
        String candidate = text.startsWith("[") ? text : sliceArray(text);
        if (candidate == null) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(candidate);
            if (root == null || !root.isArray()) {
                return List.of();
            }
            List<AiPostDraft> drafts = new ArrayList<>();
            collectDrafts(root, drafts);
            return drafts;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 递归收集草稿
     *
     * <p>模型偶尔会把每个版本再包一层数组（形如 [[{...}],[{...}]]），
     * 这里按深度优先顺序展平，最多收集 3 条</p>
     *
     * @param node   JSON 节点，可为数组或对象
     * @param drafts 收集结果
     */
    private static void collectDrafts(JsonNode node, List<AiPostDraft> drafts) {
        if (node == null || drafts.size() >= 3) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectDrafts(child, drafts);
                if (drafts.size() >= 3) {
                    return;
                }
            }
            return;
        }
        AiPostDraft draft = toDraft(node);
        if (draft != null) {
            drafts.add(draft);
        }
    }

    /**
     * 截取文本中的首个 JSON 数组片段
     *
     * @param text 模型输出
     * @return 数组片段；找不到时返回 null
     */
    private static String sliceArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        return start >= 0 && end > start ? text.substring(start, end + 1) : null;
    }

    /**
     * 把单个 JSON 节点转换为草稿
     *
     * @param node JSON 节点，支持对象或纯文本
     * @return 草稿；正文为空时返回 null
     */
    private static AiPostDraft toDraft(JsonNode node) {
        if (node == null) {
            return null;
        }
        String content;
        String style = "";
        String styleName = "";
        List<String> topics = new ArrayList<>();
        if (node.isTextual()) {
            content = node.asText("");
        } else if (node.isObject()) {
            content = node.path("content").asText("");
            style = node.path("style").asText("");
            styleName = node.path("styleName").asText("");
            JsonNode topicNode = node.path("topics");
            if (topicNode.isArray()) {
                for (JsonNode topic : topicNode) {
                    if (topic.isTextual() && !topic.asText().isBlank()) {
                        topics.add(topic.asText().trim());
                    }
                }
            }
        } else {
            return null;
        }
        String cleaned = cleanContent(content);
        if (cleaned.isBlank()) {
            return null;
        }
        return AiPostDraft.builder()
                .style(style)
                .styleName(styleName)
                .content(cleaned)
                .topics(topics)
                .build();
    }

    /**
     * 按【版本名】标签切分
     *
     * @param text 模型输出
     * @return 草稿列表，标签少于 2 个时返回空列表
     */
    private static List<AiPostDraft> parseLabels(String text) {
        Matcher matcher = LABEL.matcher(text);
        List<int[]> bounds = new ArrayList<>();
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            bounds.add(new int[]{matcher.start(), matcher.end()});
            names.add(matcher.group(1));
        }
        if (bounds.size() < 2) {
            return List.of();
        }
        List<AiPostDraft> drafts = new ArrayList<>();
        for (int i = 0; i < bounds.size() && drafts.size() < 3; i++) {
            int from = bounds.get(i)[1];
            int to = i + 1 < bounds.size() ? bounds.get(i + 1)[0] : text.length();
            String content = cleanContent(text.substring(from, Math.max(from, to)));
            if (content.isBlank()) {
                continue;
            }
            drafts.add(AiPostDraft.builder()
                    .style("")
                    .styleName(names.get(i))
                    .content(content)
                    .topics(List.of())
                    .build());
        }
        return drafts;
    }

    /**
     * 按 ||| 或空行切分
     *
     * @param text 模型输出
     * @return 草稿列表，无法切出多段时返回空列表
     */
    private static List<AiPostDraft> parseSeparators(String text) {
        String[] parts = text.split("\\|\\|\\|");
        if (parts.length < 2) {
            parts = text.split("\\n\\s*\\n");
        }
        if (parts.length < 2) {
            return List.of();
        }
        List<AiPostDraft> drafts = new ArrayList<>();
        for (String part : parts) {
            if (drafts.size() >= 3) {
                break;
            }
            String content = cleanContent(part);
            if (content.isBlank()) {
                continue;
            }
            drafts.add(AiPostDraft.builder()
                    .style("")
                    .styleName("")
                    .content(content)
                    .topics(List.of())
                    .build());
        }
        return drafts;
    }

    /**
     * 补齐风格标识与名称
     *
     * <p>风格名称统一收敛为「简约/氛围/干货」，避免模型返回「简约风」这类写法导致前端文案不一致</p>
     *
     * @param draft 草稿
     * @param index 草稿在列表中的位置
     */
    private static void applyStyle(AiPostDraft draft, int index) {
        int styleIndex = styleIndex(draft.getStyle(), draft.getStyleName());
        if (styleIndex < 0) {
            styleIndex = Math.min(index, STYLES.length - 1);
        }
        draft.setStyle(STYLES[styleIndex]);
        draft.setStyleName(STYLE_NAMES[styleIndex]);
    }

    /**
     * 从正文中抽取话题标签
     *
     * <p>模型经常把「#周末小确幸」直接写在正文里而不填 topics，
     * 这里把标签抽到 topics 并从正文移除，避免卡片里既显示正文标签又显示话题 chips；
     * 若正文只是标签、抽完为空，则保留原文不置空</p>
     *
     * @param draft 草稿
     */
    private static void extractHashtags(AiPostDraft draft) {
        String content = draft.getContent() == null ? "" : draft.getContent();
        List<String> topics = draft.getTopics() == null ? new ArrayList<>() : new ArrayList<>(draft.getTopics());
        Matcher matcher = HASHTAG.matcher(content);
        StringBuilder cleaned = new StringBuilder();
        int last = 0;
        boolean found = false;
        while (matcher.find()) {
            found = true;
            cleaned.append(content, last, matcher.start());
            String tag = matcher.group(1).trim();
            if (!tag.isEmpty() && !topics.contains(tag)) {
                topics.add(tag);
            }
            last = matcher.end();
        }
        if (!found) {
            draft.setTopics(topics);
            return;
        }
        cleaned.append(content.substring(last));
        String text = cleaned.toString().replaceAll("[ \\t]{2,}", " ").trim();
        if (!text.isBlank()) {
            draft.setContent(text);
        }
        draft.setTopics(topics);
    }

    /**
     * 根据风格标识或名称推断风格下标
     *
     * @param style     风格标识
     * @param styleName 风格名称
     * @return 0/1/2；无法识别时返回 -1
     */
    private static int styleIndex(String style, String styleName) {
        if (style != null) {
            for (int i = 0; i < STYLES.length; i++) {
                if (STYLES[i].equalsIgnoreCase(style.trim())) {
                    return i;
                }
            }
        }
        if (styleName != null) {
            for (int i = 0; i < STYLE_NAMES.length; i++) {
                if (styleName.contains(STYLE_NAMES[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 清洗正文
     *
     * <p>去掉行首版本前缀、行首标签、外层引号，压缩多余空行</p>
     *
     * @param raw 原始正文
     * @return 清洗后的正文
     */
    private static String cleanContent(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        // 只去掉 Markdown 标题（# 后必须跟空格），避免把 "#话题" 当成标题吃掉
        text = text.replaceAll("(?m)^\\s*#{1,6}[ \\t]+", "");
        text = text.replaceAll("(?m)^\\s*版本\\s*\\d*\\s*$", "");
        text = text.replaceAll("(?m)^\\s*\\|\\|\\|\\s*$", "");
        text = text.replaceAll("^\\s*版本\\s*\\d*\\s*", "");
        text = LEADING_LABEL.matcher(text).replaceFirst("");
        text = text.replaceAll("^[\"'“”‘’「」]+", "").replaceAll("[\"'“”‘’「」]+$", "");
        text = text.replaceAll("\\s*\\|\\|\\|\\s*$", "");
        text = text.replaceAll("[ \t]+\n", "\n");
        text = text.replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }
}
