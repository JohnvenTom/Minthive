package com.minthive.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 审计快照脱敏与截断单元测试
 */
class AuditRedactorTest {

    private final AuditProperties properties = new AuditProperties();

    private final AuditRedactor redactor = new AuditRedactor(
            new ObjectMapper().registerModule(new JavaTimeModule()), properties);

    @Test
    void credentialColumnsAreMaskedAndReported() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("password", "$2b$10$hash");
        row.put("access_token", "secret-token");
        row.put("sms_code", "123456");
        row.put("nickname", "alice");

        AuditRedactor.Snapshot snapshot = redactor.fromRow(row);

        assertNotNull(snapshot);
        assertFalse(snapshot.json().contains("$2b$10$hash"));
        assertFalse(snapshot.json().contains("secret-token"));
        assertFalse(snapshot.json().contains("123456"));
        assertTrue(snapshot.json().contains("[MASKED]"));
        assertTrue(snapshot.maskedColumns().contains("password"));
        assertTrue(snapshot.maskedColumns().contains("access_token"));
        assertTrue(snapshot.maskedColumns().contains("sms_code"));
        assertFalse(snapshot.truncated());
    }

    @Test
    void oversizedSnapshotIsTruncatedIntoSafeJson() {
        properties.setSnapshotMaxBytes(512);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 2L);
        row.put("content", "x".repeat(2000));

        AuditRedactor.Snapshot snapshot = redactor.fromRow(row);

        assertNotNull(snapshot);
        assertTrue(snapshot.truncated());
        assertTrue(snapshot.json().contains("__truncated"));
        assertTrue(snapshot.json().length() < 1200);
    }

    @Test
    void sqlDigestIsStableAcrossWhitespace() {
        String first = AuditRedactor.sqlDigest("UPDATE user\n  SET nickname = ? WHERE id = ?");
        String second = AuditRedactor.sqlDigest("update user set nickname = ? where id = ?");
        assertEquals(first, second);
        assertEquals(64, first.length());
    }

    @Test
    void anonymousIdentityHashIsStableAndNotPlaintext() {
        String hash = AuditIdentity.sha256("13800138000");
        assertEquals(hash, AuditIdentity.sha256("13800138000"));
        assertEquals(64, hash.length());
        assertFalse(hash.contains("13800138000"));
    }
}
