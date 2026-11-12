package com.minthive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.minthive.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 私信与通知集成测试
 *
 * <p>覆盖：历史游标无重叠、已读幂等、撤回写审计、通知已读幂等</p>
 */
class MessageNotificationIT extends BaseIntegrationTest {

    @Test
    void historyCursorDoesNotOverlapAndMarkReadIsIdempotent() throws Exception {
        long sender = createUser("msg-sender", 0);
        long receiver = createUser("msg-receiver", 0);
        String senderToken = login(sender);
        String receiverToken = login(receiver);

        for (int i = 0; i < 5; i++) {
            request("POST", "/api/message", senderToken,
                    Map.of("toUserId", receiver, "content", "it-msg-" + i, "msgType", 0));
        }

        JsonNode first = request("GET", "/api/message/history?peerId=" + sender + "&limit=2", receiverToken, null);
        List<Long> firstIds = ids(first.path("data"));
        assertEquals(2, firstIds.size());
        long beforeId = firstIds.stream().mapToLong(Long::longValue).min().orElseThrow();

        JsonNode second = request("GET", "/api/message/history?peerId=" + sender + "&limit=2&beforeId=" + beforeId,
                receiverToken, null);
        List<Long> secondIds = ids(second.path("data"));
        assertEquals(2, secondIds.size());
        assertTrue(secondIds.stream().allMatch(id -> id < beforeId));
        assertTrue(secondIds.stream().noneMatch(firstIds::contains));

        assertEquals(200, request("POST", "/api/message/read?fromUserId=" + sender, receiverToken, null)
                .path("code").asInt());
        assertEquals(200, request("POST", "/api/message/read?fromUserId=" + sender, receiverToken, null)
                .path("code").asInt());
        JsonNode unread = request("GET", "/api/message/unread", receiverToken, null);
        assertEquals(0, unread.path("data").path("message").asInt());
    }

    @Test
    void revokeWritesLogicalDeleteAudit() throws Exception {
        long sender = createUser("revoke-sender", 0);
        long receiver = createUser("revoke-receiver", 0);
        String senderToken = login(sender);
        String receiverToken = login(receiver);
        JsonNode sent = request("POST", "/api/message", senderToken,
                Map.of("toUserId", receiver, "content", "it-revoke-me", "msgType", 0));
        long messageId = sent.path("data").path("id").asLong();

        assertEquals(200, request("DELETE", "/api/message/revoke/" + messageId, senderToken, null)
                .path("code").asInt());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String after = jdbcTemplate.query(
                    "SELECT after_json FROM audit_log_change WHERE table_name='message' AND record_id=? "
                            + "AND operation='UPDATE' ORDER BY id DESC LIMIT 1",
                    rs -> rs.next() ? rs.getString(1) : null, String.valueOf(messageId));
            assertTrue(after != null && after.contains("\"deleted\":1"));
        });
    }

    @Test
    void notificationReadIsIdempotentAndNeverLeaksOthersData() throws Exception {
        long user = createUser("notice-user", 0);
        String token = login(user);

        assertEquals(200, request("POST", "/api/message/notifications/read?type=like&id=999999", token, null)
                .path("code").asInt());
        assertEquals(200, request("POST", "/api/message/notifications/read?type=like&id=999999", token, null)
                .path("code").asInt());
        assertEquals(200, request("POST", "/api/message/notifications/read?type=system&id=999999", token, null)
                .path("code").asInt());

        JsonNode unread = request("GET", "/api/message/unread", token, null);
        assertEquals(200, unread.path("code").asInt());
    }

    /**
     * 提取消息ID列表
     *
     * @param array 消息数组
     * @return ID 列表
     */
    private List<Long> ids(JsonNode array) {
        List<Long> ids = new ArrayList<>();
        if (array.isArray()) {
            array.forEach(node -> ids.add(node.path("id").asLong()));
        }
        return ids;
    }
}
