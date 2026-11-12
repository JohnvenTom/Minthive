package com.minthive.security;

import com.minthive.media.MediaLocator;
import com.minthive.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 媒体授权与审计脱敏集成测试
 *
 * <p>覆盖：审计落库与密码脱敏、GET 不产生审计、媒体内容必须携带有效票据</p>
 */
class MediaAuditIT extends BaseIntegrationTest {

    @Autowired
    private MediaLocator mediaLocator;

    @Test
    void auditMasksPasswordAndGetIsNotRecorded() throws Exception {
        long user = createUser("audit-user", 0);
        String token = login(user);
        request("PUT", "/api/user", token, Map.of("nickname", "audit-renamed"));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String before = jdbcTemplate.query(
                    "SELECT c.before_json FROM audit_log_change c "
                            + "JOIN audit_log l ON c.audit_log_id = l.id "
                            + "WHERE c.table_name='user' AND l.operator_id=? ORDER BY c.id DESC LIMIT 1",
                    rs -> rs.next() ? rs.getString(1) : null, user);
            assertNotNull(before);
            assertTrue(before.contains("[MASKED]"));
            assertFalse(before.contains("$2b$"));
        });

        long postId = createPost(user, 0, null, 1);
        // 等待异步写入队列排空后再取基线，避免把登录/PUT 的延迟落库误判为 GET 产生审计
        long baseline = stableAuditCount();
        request("GET", "/api/post/" + postId, token, null);
        long afterGet = stableAuditCount();
        assertEquals(baseline, afterGet, "GET 请求不应产生审计记录");
    }

    /**
     * 等待审计异步队列排空（连续两次计数一致）
     *
     * @return 稳定后的审计总数
     * @throws InterruptedException 等待被中断
     */
    private long stableAuditCount() throws InterruptedException {
        long last = -1;
        for (int i = 0; i < 10; i++) {
            long current = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class);
            if (current == last) {
                return current;
            }
            last = current;
            Thread.sleep(300);
        }
        return last;
    }

    @Test
    void mediaContentRejectsMissingOrForgedTicket() throws Exception {
        long user = createUser("media-user", 0);
        String objectName = "users/" + user + "/" + "a".repeat(32) + ".png";
        String token = mediaLocator.fromName(objectName).token();

        mockMvc.perform(get("/api/file/content/" + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/file/content/" + token).param("ticket", "m1.0.1.AAAA"))
                .andExpect(status().isForbidden());
    }
}
