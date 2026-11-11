package com.minthive.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.mapping.MappedStatement;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 审计表元数据解析器
 *
 * <p>功能描述：根据 MappedStatement 的命名空间反推 BaseMapper 实体类型，
 * 得到真实表名、主键列与字段列，避免从 SQL 字符串猜测表结构</p>
 * <p>注意事项：自定义 XML/注解 SQL（非 BaseMapper）返回 null，由调用方降级为 SQL 摘要</p>
 */
@Component
public class AuditTableResolver {

    /** 从 SQL 头部解析表名（兜底用，表名由服务端生成，不含用户输入） */
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?is)^\\s*(?:insert\\s+into|update|delete\\s+from)\\s+`?([a-zA-Z0-9_]+)`?");

    private final ConcurrentHashMap<String, Optional<TableInfo>> cache = new ConcurrentHashMap<>();

    /**
     * 解析 MappedStatement 对应的表元数据
     *
     * @param statement MyBatis 映射语句
     * @return 表元数据；无法解析时返回 null
     */
    public TableInfo resolve(MappedStatement statement) {
        String id = statement.getId();
        int idx = id.lastIndexOf('.');
        if (idx <= 0) {
            return null;
        }
        String namespace = id.substring(0, idx);
        return cache.computeIfAbsent(namespace, this::load).orElse(null);
    }

    /**
     * 从 SQL 文本头部解析表名
     *
     * @param sql SQL 文本
     * @return 表名；无法解析时返回 null
     */
    public static String parseTable(String sql) {
        if (sql == null) {
            return null;
        }
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        return matcher.find() ? matcher.group(1).toLowerCase() : null;
    }

    /**
     * 加载并缓存命名空间对应的表元数据
     *
     * @param namespace Mapper 接口全限定名
     * @return 表元数据
     */
    private Optional<TableInfo> load(String namespace) {
        try {
            Class<?> mapper = Class.forName(namespace);
            for (Type type : mapper.getGenericInterfaces()) {
                if (!(type instanceof ParameterizedType parameterized)
                        || parameterized.getRawType() != BaseMapper.class) {
                    continue;
                }
                Type entityType = parameterized.getActualTypeArguments()[0];
                TableInfo info = entityType instanceof Class<?> entity
                        ? TableInfoHelper.getTableInfo(entity) : null;
                if (info != null) {
                    return Optional.of(info);
                }
            }
        } catch (Exception ignored) {
            // 非 BaseMapper 或类未加载，走 SQL 摘要降级
        }
        return Optional.empty();
    }
}
