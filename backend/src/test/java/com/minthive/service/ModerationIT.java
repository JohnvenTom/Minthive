package com.minthive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.minthive.ai.AiService;
import com.minthive.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 审核状态机集成测试
 *
 * <p>覆盖：审核通过、审核不可用转待审、图片审核不可用转待审、编辑重新审核、草稿不进公开流</p>
 */
class ModerationIT extends BaseIntegrationTest {

    @Test
    void publishPassesWhenModerationAvailableAndSafe() throws Exception {
        long user = createUser("moderation-ok", 0);
        String token = login(user);

        JsonNode result = request("POST", "/api/post", token, Map.of("content", "hello mint"));
        assertEquals(200, result.path("code").asInt());
        assertEquals(1, result.path("data").path("auditStatus").asInt());
    }

    @Test
    void unavailableModerationKeepsPostPending() throws Exception {
        when(aiContext.detectContent(any(), any()))
                .thenReturn(AiService.AiDetectResult.unavailable("down"));
        long user = createUser("moderation-down", 0);
        String token = login(user);

        JsonNode result = request("POST", "/api/post", token, Map.of("content", "pending please"));
        assertEquals(200, result.path("code").asInt());
        assertEquals(0, result.path("data").path("auditStatus").asInt());
    }

    @Test
    void unavailableImageModerationKeepsPostPending() throws Exception {
        when(imageModerationService.moderate(any(), anyString()))
                .thenReturn(AiService.AiDetectResult.unavailable("image down"));
        long user = createUser("image-moderation", 0);
        String token = login(user);

        JsonNode result = request("POST", "/api/post", token,
                Map.of("content", "with image", "images", "[\"https://example.com/a.png\"]"));
        assertEquals(200, result.path("code").asInt());
        assertEquals(0, result.path("data").path("auditStatus").asInt());
    }

    @Test
    void editingContentReRunsModeration() throws Exception {
        long user = createUser("moderation-edit", 0);
        String token = login(user);
        JsonNode created = request("POST", "/api/post", token, Map.of("content", "safe content"));
        long postId = created.path("data").path("id").asLong();
        assertEquals(1, created.path("data").path("auditStatus").asInt());

        when(aiContext.detectContent(any(), any()))
                .thenReturn(new AiService.AiDetectResult(true, "广告", "high"));
        JsonNode updated = request("PUT", "/api/post/" + postId, token, Map.of("content", "bad content"));
        assertEquals(200, updated.path("code").asInt());
        assertEquals(0, updated.path("data").path("auditStatus").asInt());
    }

    @Test
    void draftNeverAppearsInPublicPage() throws Exception {
        long user = createUser("draft-user", 0);
        String token = login(user);
        JsonNode draft = request("POST", "/api/post/draft", token, Map.of("content", "draft content"));
        long draftId = draft.path("data").path("id").asLong();

        JsonNode page = request("GET", "/api/post/page?current=1&size=100", null, null);
        List<JsonNode> records = new java.util.ArrayList<>();
        page.path("data").path("records").forEach(records::add);
        assertFalse(records.stream().anyMatch(node -> node.path("id").asLong() == draftId));
    }
}
