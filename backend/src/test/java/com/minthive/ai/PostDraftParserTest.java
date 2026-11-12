package com.minthive.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 帖子文案解析器单元测试
 *
 * <p>覆盖：JSON 数组、Markdown 代码块、版本标签分段、分隔符分段、
 * 引号与版本前缀清洗、无法识别时返回空列表</p>
 */
class PostDraftParserTest {

    @Test
    void parsesJsonArrayInsideCodeFence() {
        String raw = """
                ```json
                [
                  {"style":"simple","styleName":"简约","content":"周末爬山，放松心情。","topics":["爬山"]},
                  {"style":"vibe","styleName":"氛围","content":"山风拂面，\\n一切都慢了下来。","topics":[]},
                  {"style":"pro","styleName":"干货","content":"出发前记得看天气。","topics":["路线"]}
                ]
                ```
                """;

        List<AiPostDraft> drafts = PostDraftParser.parse(raw);

        assertEquals(3, drafts.size());
        assertEquals("simple", drafts.get(0).getStyle());
        assertEquals("简约", drafts.get(0).getStyleName());
        assertEquals(List.of("爬山"), drafts.get(0).getTopics());
        assertTrue(drafts.get(1).getContent().contains("\n"), "正文应保留换行");
        assertEquals("pro", drafts.get(2).getStyle());
    }

    @Test
    void parsesLabelSectionsAndStripsPrefixes() {
        String raw = """
                版本1【简约风】：
                "周末爬山，简单快乐。"
                |||
                版本2【氛围感】：
                山风拂面，一切都慢了下来。
                |||
                版本3【干货型】：
                记得带水和登山杖。
                |||
                多余内容不应渲染
                """;

        List<AiPostDraft> drafts = PostDraftParser.parse(raw);

        assertEquals(3, drafts.size());
        assertEquals("简约", drafts.get(0).getStyleName());
        assertEquals("周末爬山，简单快乐。", drafts.get(0).getContent());
        assertEquals("氛围", drafts.get(1).getStyleName());
        assertEquals("干货", drafts.get(2).getStyleName());
        assertFalse(drafts.toString().contains("版本"), "正文不应残留版本前缀");
        assertFalse(drafts.get(0).getContent().startsWith("\""), "正文不应保留外层引号");
    }

    @Test
    void parsesSeparatorOutputAndOrdersStyles() {
        String raw = "第一版文案|||第二版文案|||第三版文案";

        List<AiPostDraft> drafts = PostDraftParser.parse(raw);

        assertEquals(3, drafts.size());
        assertEquals("simple", drafts.get(0).getStyle());
        assertEquals("vibe", drafts.get(1).getStyle());
        assertEquals("pro", drafts.get(2).getStyle());
        assertEquals("氛围", drafts.get(1).getStyleName());
    }

    @Test
    void flattensNestedArraysFromModel() {
        String raw = "[[{\"style\":\"simple\",\"content\":\"第一版\"}],[{\"style\":\"vibe\",\"content\":\"第二版\"}],"
                + "[{\"style\":\"pro\",\"content\":\"第三版\"}]]";

        List<AiPostDraft> drafts = PostDraftParser.parse(raw);

        assertEquals(3, drafts.size());
        assertEquals("第一版", drafts.get(0).getContent());
        assertEquals("氛围", drafts.get(1).getStyleName());
        assertEquals("第三版", drafts.get(2).getContent());
    }

    @Test
    void extractsHashtagsFromContentIntoTopics() {
        String raw = "[{\"style\":\"simple\",\"content\":\"每次点快餐都幸福！#快餐控 #美食分享\"}]";

        List<AiPostDraft> drafts = PostDraftParser.parse(raw);

        assertEquals(1, drafts.size());
        assertEquals("每次点快餐都幸福！", drafts.get(0).getContent());
        assertEquals(List.of("快餐控", "美食分享"), drafts.get(0).getTopics());
    }

    @Test
    void mergesProvidedTopicsWithExtractedAndDeduplicates() {
        String raw = "[{\"style\":\"vibe\",\"content\":\"好吃到停不下来 #美食\",\"topics\":[\"美食\",\"推荐\"]}]";

        List<AiPostDraft> drafts = PostDraftParser.parse(raw);

        assertEquals("好吃到停不下来", drafts.get(0).getContent());
        assertEquals(List.of("美食", "推荐"), drafts.get(0).getTopics());
    }

    @Test
    void keepsContentWhenItOnlyContainsHashtags() {
        List<AiPostDraft> drafts = PostDraftParser.parse("[{\"content\":\"#美食\"}]");

        assertEquals("#美食", drafts.get(0).getContent());
        assertEquals(List.of("美食"), drafts.get(0).getTopics());
    }

    @Test
    void keepsFewerDraftsInsteadOfPadding() {
        List<AiPostDraft> drafts = PostDraftParser.parse("[{\"content\":\"只有一条\"}]");

        assertEquals(1, drafts.size());
        assertEquals("只有一条", drafts.get(0).getContent());
        assertEquals("simple", drafts.get(0).getStyle());
    }

    @Test
    void returnsEmptyForUnrecognizableText() {
        assertTrue(PostDraftParser.parse("就是一段普通的话，没有版本结构").isEmpty());
        assertTrue(PostDraftParser.parse("").isEmpty());
        assertTrue(PostDraftParser.parse(null).isEmpty());
    }
}
