package com.minthive.audit;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.reflection.SystemMetaObject;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 审计快照脱敏与序列化工具
 *
 * <p>功能描述：把数据库行或实体转换为可落库的 JSON 快照，并按规则剔除凭证类列、限制长度</p>
 * <p>注意事项：凭证列只保留字段名并写入 maskedColumns，原文绝不进入审计库</p>
 */
@Component
@RequiredArgsConstructor
public class AuditRedactor {

    /** 凭证类列名：password/token/secret/code（大小写不敏感） */
    private static final Pattern SENSITIVE_COLUMN = Pattern.compile("(?i)(password|token|secret|code)");

    /** 脱敏占位符 */
    private static final String MASKED = "[MASKED]";

    /** 二进制列占位符 */
    private static final String BINARY = "[BINARY]";

    private final ObjectMapper objectMapper;

    private final AuditProperties properties;

    /**
     * 快照结果
     *
     * @param json          序列化后的快照
     * @param maskedColumns 被脱敏列名（逗号分隔，可为空）
     * @param truncated     是否发生截断
     */
    public record Snapshot(String json, String maskedColumns, boolean truncated) {
    }

    /**
     * 对数据库行做脱敏并序列化
     *
     * @param row 数据库行（列名 → 值），可为空
     * @return 快照；入参为空时返回 null
     */
    public Snapshot fromRow(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        List<String> masked = new ArrayList<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String column = entry.getKey();
            if (SENSITIVE_COLUMN.matcher(column).find()) {
                safe.put(column, MASKED);
                masked.add(column);
            } else {
                safe.put(column, normalize(entry.getValue()));
            }
        }
        return serialize(safe, masked);
    }

    /**
     * 把实体当前值转换为列名 → 值 的映射
     *
     * @param entity    实体对象
     * @param tableInfo MyBatis-Plus 表元数据
     * @return 列映射；实体为空时返回空 Map
     */
    public Map<String, Object> entityRow(Object entity, TableInfo tableInfo) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (entity == null || tableInfo == null) {
            return row;
        }
        var meta = SystemMetaObject.forObject(entity);
        if (tableInfo.getKeyProperty() != null && meta.hasGetter(tableInfo.getKeyProperty())) {
            row.put(tableInfo.getKeyColumn(), normalize(meta.getValue(tableInfo.getKeyProperty())));
        }
        for (TableFieldInfo field : tableInfo.getFieldList()) {
            if (meta.hasGetter(field.getProperty())) {
                Object value = meta.getValue(field.getProperty());
                if (value != null) {
                    row.put(field.getColumn(), normalize(value));
                }
            }
        }
        return row;
    }

    /**
     * 合并“变更前快照”与实体非空字段，得到变更后快照
     *
     * @param before    变更前快照（列名 → 值）
     * @param entity    本次更新实体
     * @param tableInfo 表元数据
     * @return 变更后列映射
     */
    public Map<String, Object> mergeAfter(Map<String, Object> before, Object entity, TableInfo tableInfo) {
        Map<String, Object> after = before == null ? new LinkedHashMap<>() : new LinkedHashMap<>(before);
        if (entity == null || tableInfo == null) {
            return after;
        }
        var meta = SystemMetaObject.forObject(entity);
        if (tableInfo.getKeyProperty() != null && meta.hasGetter(tableInfo.getKeyProperty())
                && meta.getValue(tableInfo.getKeyProperty()) != null) {
            after.put(tableInfo.getKeyColumn(), normalize(meta.getValue(tableInfo.getKeyProperty())));
        }
        for (TableFieldInfo field : tableInfo.getFieldList()) {
            if (!meta.hasGetter(field.getProperty())) {
                continue;
            }
            Object value = meta.getValue(field.getProperty());
            if (value != null) {
                after.put(field.getColumn(), normalize(value));
            }
        }
        return after;
    }

    /**
     * 将列映射序列化为受长度限制的快照
     *
     * @param row    列映射
     * @param masked 被脱敏列名
     * @return 快照；row 为空时返回 null
     */
    public Snapshot serialize(Map<String, Object> row, List<String> masked) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(row);
            boolean truncated = json.getBytes(StandardCharsets.UTF_8).length > properties.getSnapshotMaxBytes();
            if (truncated) {
                json = truncatedJson(json);
            }
            return new Snapshot(json, masked == null || masked.isEmpty() ? null : String.join(",", masked), truncated);
        } catch (Exception e) {
            return new Snapshot("{\"__error\":\"snapshot serialization failed\"}", null, true);
        }
    }

    /**
     * 计算 SQL 模板摘要
     *
     * @param sql 原始 SQL
     * @return SHA-256 摘要；SQL 为空时返回 null
     */
    public static String sqlDigest(String sql) {
        if (sql == null || sql.isBlank()) {
            return null;
        }
        String normalized = sql.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成合法的截断 JSON
     *
     * @param json 原始 JSON
     * @return 含 truncated 标记与预览片段的 JSON
     */
    private String truncatedJson(String json) {
        int previewLength = Math.max(256, properties.getSnapshotMaxBytes() / 2);
        String preview = json.length() > previewLength ? json.substring(0, previewLength) : json;
        ObjectNode node = objectMapper.createObjectNode();
        node.put("__truncated", true);
        node.put("preview", preview);
        return node.toString();
    }

    /**
     * 规范化 JDBC 返回的特殊类型
     *
     * @param value 原始值
     * @return 可安全序列化的值
     */
    private Object normalize(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof byte[]) {
            return BINARY;
        }
        return value;
    }
}
