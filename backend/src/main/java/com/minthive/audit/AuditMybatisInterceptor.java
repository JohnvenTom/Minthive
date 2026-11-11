package com.minthive.audit;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * MyBatis 表变更审计拦截器
 *
 * <p>功能描述：拦截 INSERT/UPDATE/DELETE，解析表名与主键，回查变更前整行，
 * 结合本次实体计算变更后快照，并在事务提交后加入当前请求的审计上下文</p>
 * <p>注意事项：</p>
 * <ul>
 *   <li>审计表自身永远跳过，避免异步写入递归采集；</li>
 *   <li>GET 请求触发的写入不采集；无 HTTP 上下文时按 SYSTEM 采集；</li>
 *   <li>wrapper/批量更新无法定位唯一主键时降级为影响行数 + SQL 摘要；</li>
 *   <li>只有事务提交后的变更才进入审计，回滚的变更不会落库。</li>
 * </ul>
 */
@Component
@Intercepts(@Signature(type = Executor.class, method = "update",
        args = {MappedStatement.class, Object.class}))
public class AuditMybatisInterceptor implements Interceptor {

    /** 审计表自身，任何情况下都不采集 */
    private static final Set<String> AUDIT_TABLES = Set.of("audit_log", "audit_log_change");

    private final AuditProperties properties;

    private final AuditTableResolver resolver;

    private final AuditRedactor redactor;

    /**
     * 延迟获取写入器：AuditWriter 依赖 Mapper，而 Mapper 依赖 SqlSessionFactory，
     * 直接注入会与拦截器注册形成循环依赖
     */
    private final ObjectProvider<AuditWriter> writerProvider;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造器注入依赖
     *
     * @param properties     审计配置
     * @param resolver       表元数据解析器
     * @param redactor       快照脱敏工具
     * @param writerProvider 异步写入器（延迟获取，避免循环依赖）
     * @param jdbcTemplate   JDBC 模板（复用当前事务连接回查旧行）
     */
    public AuditMybatisInterceptor(AuditProperties properties, AuditTableResolver resolver,
                                   AuditRedactor redactor, ObjectProvider<AuditWriter> writerProvider,
                                   JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.resolver = resolver;
        this.redactor = redactor;
        this.writerProvider = writerProvider;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.isEnabled()) {
            return invocation.proceed();
        }
        Object[] args = invocation.getArgs();
        MappedStatement statement = (MappedStatement) args[0];
        Object parameter = args.length > 1 ? args[1] : null;
        SqlCommandType command = statement.getSqlCommandType();
        if (!isWrite(command)) {
            return invocation.proceed();
        }

        TableInfo tableInfo = resolver.resolve(statement);
        BoundSql boundSql;
        try {
            boundSql = statement.getBoundSql(parameter);
        } catch (Exception e) {
            // 动态 SQL 解析异常时放弃本次审计，绝不阻断业务写入
            return invocation.proceed();
        }
        String table = tableInfo != null ? tableInfo.getTableName() : AuditTableResolver.parseTable(boundSql.getSql());
        if (table == null || isExcluded(table)) {
            return invocation.proceed();
        }

        AuditScope scope = AuditScope.current();
        boolean system = scope == null;
        if (!system && !scope.isAuditable()) {
            return invocation.proceed();
        }
        if (system) {
            scope = AuditScope.beginSystem(Thread.currentThread().getName());
        }

        String operation = command.name();
        Object entity = extractEntity(parameter);
        String recordId = resolveRecordId(parameter, entity);
        Map<String, Object> before = "INSERT".equals(operation) || recordId == null || tableInfo == null
                ? null : selectBefore(tableInfo, recordId);

        Object result;
        try {
            result = invocation.proceed();
        } catch (Throwable error) {
            record(scope, system, buildChange(operation, tableInfo, table, recordId, 0, before, null, boundSql));
            throw error;
        }

        int affected = result instanceof Number number ? number.intValue() : 0;
        Map<String, Object> after = resolveAfter(operation, before, entity, tableInfo, statement);
        if ("INSERT".equals(operation) && recordId == null) {
            recordId = resolveRecordId(parameter, entity);
        }
        record(scope, system, buildChange(operation, tableInfo, table, recordId, affected, before, after, boundSql));
        return result;
    }

    /**
     * 计算变更后快照
     *
     * @param operation 操作类型
     * @param before    变更前整行
     * @param entity    本次实体
     * @param tableInfo 表元数据
     * @param statement 映射语句
     * @return 变更后整行；无法计算时返回 null
     */
    private Map<String, Object> resolveAfter(String operation, Map<String, Object> before, Object entity,
                                             TableInfo tableInfo, MappedStatement statement) {
        if ("INSERT".equals(operation)) {
            return entity == null ? null : redactor.entityRow(entity, tableInfo);
        }
        if (before == null) {
            return null;
        }
        if (entity != null) {
            return redactor.mergeAfter(before, entity, tableInfo);
        }
        // 逻辑删除：MP 将 deleteById 改写为 UPDATE ... SET deleted=1，用旧行推导新值
        if ("UPDATE".equals(operation) && tableInfo != null && tableInfo.getLogicDeleteFieldInfo() != null) {
            TableFieldInfo logic = tableInfo.getLogicDeleteFieldInfo();
            Map<String, Object> after = new LinkedHashMap<>(before);
            after.put(logic.getColumn(), 1);
            return after;
        }
        return null;
    }

    /**
     * 构造变更记录（完成脱敏与截断）
     *
     * @param operation 操作类型
     * @param tableInfo 表元数据（可为空）
     * @param table     表名
     * @param recordId  主键值
     * @param affected  影响行数
     * @param before    变更前整行
     * @param after     变更后整行
     * @param boundSql  SQL 信息
     * @return 变更记录
     */
    private AuditChangeRecord buildChange(String operation, TableInfo tableInfo, String table, String recordId,
                                          int affected, Map<String, Object> before, Map<String, Object> after,
                                          BoundSql boundSql) {
        AuditRedactor.Snapshot beforeSnap = before == null ? null : redactor.fromRow(before);
        AuditRedactor.Snapshot afterSnap = after == null ? null : redactor.fromRow(after);
        String masked = joinMasked(beforeSnap, afterSnap);
        boolean truncated = (beforeSnap != null && beforeSnap.truncated())
                || (afterSnap != null && afterSnap.truncated());
        // 无表元数据或无法解析主键时，只保留 SQL 摘要，避免伪造行级快照
        boolean degrade = tableInfo == null || recordId == null;
        return new AuditChangeRecord(
                table,
                recordId,
                operation,
                affected,
                beforeSnap == null ? null : beforeSnap.json(),
                afterSnap == null ? null : afterSnap.json(),
                masked,
                truncated,
                degrade ? AuditRedactor.sqlDigest(boundSql.getSql()) : null);
    }

    /**
     * 合并两条快照的脱敏列名
     *
     * @param before 前快照
     * @param after  后快照
     * @return 逗号分隔的列名，可为空
     */
    private String joinMasked(AuditRedactor.Snapshot before, AuditRedactor.Snapshot after) {
        String b = before == null ? null : before.maskedColumns();
        String a = after == null ? null : after.maskedColumns();
        if (b == null) {
            return a;
        }
        if (a == null || a.equals(b)) {
            return b;
        }
        return b + "," + a;
    }

    /**
     * 事务提交后把变更加入上下文；无事务时立即加入
     *
     * @param scope   审计上下文
     * @param system  是否为后台任务上下文
     * @param change  变更记录
     */
    private void record(AuditScope scope, boolean system, AuditChangeRecord change) {
        Consumer<Void> action = ignored -> {
            scope.addChange(change);
            if (system) {
                flushSystem(scope);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.accept(null);
                }
            });
        } else {
            action.accept(null);
        }
    }

    /**
     * 后台任务：立即组装并投递当前系统审计事件
     *
     * @param scope 系统审计上下文
     */
    private void flushSystem(AuditScope scope) {
        long duration = (System.nanoTime() - scope.getStartNanos()) / 1_000_000L;
        var log = scope.toAuditLog("SYSTEM", null, null, MDC.get("traceId"), null, (int) duration);
        writerProvider.getObject().submit(AuditEventFactory.event(log, scope.drainChanges()));
        // 只清理当前线程自己的上下文，避免误删同线程后续语句新建的 scope
        if (AuditScope.current() == scope) {
            AuditScope.clear();
        }
    }

    /**
     * 判断是否为写操作
     *
     * @param command SQL 命令类型
     * @return true=写操作
     */
    private boolean isWrite(SqlCommandType command) {
        return command == SqlCommandType.INSERT
                || command == SqlCommandType.UPDATE
                || command == SqlCommandType.DELETE;
    }

    /**
     * 判断表是否被排除
     *
     * @param table 表名
     * @return true=跳过审计
     */
    private boolean isExcluded(String table) {
        String normalized = table.toLowerCase(Locale.ROOT);
        if (AUDIT_TABLES.contains(normalized)) {
            return true;
        }
        return properties.getExcludeTables() != null && properties.getExcludeTables().stream()
                .filter(java.util.Objects::nonNull)
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .anyMatch(normalized::equals);
    }

    /**
     * 在当前事务连接上回查变更前整行
     *
     * @param tableInfo 表元数据
     * @param recordId  主键值
     * @return 列名 → 值 的映射；不存在或查询失败返回 null
     */
    private Map<String, Object> selectBefore(TableInfo tableInfo, String recordId) {
        if (tableInfo.getKeyColumn() == null || recordId == null) {
            return null;
        }
        String sql = "SELECT * FROM `" + tableInfo.getTableName() + "` WHERE `"
                + tableInfo.getKeyColumn() + "` = ? LIMIT 1";
        try {
            return jdbcTemplate.queryForMap(sql, recordId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从参数中提取本次写入实体
     *
     * @param parameter Mapper 方法参数
     * @return 实体；批量/非实体参数返回 null
     */
    private Object extractEntity(Object parameter) {
        if (parameter == null) {
            return null;
        }
        if (parameter instanceof Map<?, ?> map) {
            Object entity = map.get("et");
            return entity instanceof Map || entity == null ? null : entity;
        }
        if (parameter instanceof Number || parameter instanceof CharSequence
                || parameter instanceof java.util.Collection<?>
                || parameter instanceof com.baomidou.mybatisplus.core.conditions.Wrapper) {
            return null;
        }
        return parameter;
    }

    /**
     * 解析本次变更的主键值
     *
     * @param parameter Mapper 方法参数
     * @param entity    写入实体
     * @return 主键字符串；无法解析返回 null
     */
    private String resolveRecordId(Object parameter, Object entity) {
        if (entity != null) {
            return readId(entity);
        }
        if (parameter instanceof Number || parameter instanceof CharSequence) {
            return String.valueOf(parameter);
        }
        if (parameter instanceof Map<?, ?> map) {
            Object id = map.get("id");
            if (id != null) {
                return String.valueOf(id);
            }
            Object et = map.get("et");
            if (et != null) {
                return readId(et);
            }
        }
        return null;
    }

    /**
     * 读取对象 id 属性
     *
     * @param object 实体对象
     * @return id 字符串；无 id 返回 null
     */
    private String readId(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Number || object instanceof CharSequence) {
            return String.valueOf(object);
        }
        try {
            var meta = SystemMetaObject.forObject(object);
            if (meta.hasGetter("id")) {
                Object id = meta.getValue("id");
                return id == null ? null : String.valueOf(id);
            }
        } catch (Exception ignored) {
            // 忽略无法读取 id 的对象，走 SQL 摘要降级
        }
        return null;
    }
}
