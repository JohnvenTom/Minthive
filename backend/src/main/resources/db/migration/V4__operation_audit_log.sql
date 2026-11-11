-- 操作审计主表：一次非 GET 写请求一行，记录操作者、来源、结果与耗时
CREATE TABLE IF NOT EXISTS audit_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    trace_id       VARCHAR(64)  NULL COMMENT '请求链路ID',
    operator_type  VARCHAR(16)  NOT NULL COMMENT '操作主体: USER/ADMIN/ANONYMOUS/SYSTEM',
    operator_id    BIGINT       NULL COMMENT '操作者用户ID(匿名/系统为空)',
    operator_role  TINYINT      NULL COMMENT '操作时的数据库角色: 0普通 1圈主 2管理员',
    operator_label VARCHAR(128) NULL COMMENT '匿名尝试账号或系统任务名',
    identity_hash  CHAR(64)     NULL COMMENT '匿名身份指纹 SHA-256(手机号)',
    source         VARCHAR(16)  NOT NULL COMMENT '来源: HTTP/SYSTEM',
    http_method    VARCHAR(8)   NULL COMMENT 'HTTP 方法',
    request_path   VARCHAR(512) NULL COMMENT '请求路径',
    request_query  VARCHAR(1024) NULL COMMENT '查询串(已脱敏)',
    client_ip      VARCHAR(64)  NULL COMMENT '可信代理解析出的客户端IP',
    remote_addr    VARCHAR(64)  NULL COMMENT '直接连接地址',
    forwarded_for  VARCHAR(512) NULL COMMENT 'X-Forwarded-For 原文',
    user_agent     VARCHAR(512) NULL COMMENT 'User-Agent',
    result_code    INT          NULL COMMENT '业务响应码',
    result_msg     VARCHAR(255) NULL COMMENT '业务响应消息',
    http_status    INT          NULL COMMENT 'HTTP 状态码',
    duration_ms    INT          NULL COMMENT '请求耗时毫秒',
    change_count   INT          NOT NULL DEFAULT 0 COMMENT '变更条数',
    truncated      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否存在被截断的快照',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    KEY idx_audit_time (create_time),
    KEY idx_audit_operator (operator_type, operator_id, create_time),
    KEY idx_audit_trace (trace_id),
    KEY idx_audit_path (request_path, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='操作审计主表';

-- 操作审计变更子表：一条 SQL 变更一行，保存表名、主键与字段级前后快照
CREATE TABLE IF NOT EXISTS audit_log_change (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    audit_log_id   BIGINT       NOT NULL COMMENT '所属审计主记录',
    table_name     VARCHAR(64)  NOT NULL COMMENT '被修改的表名',
    record_id      VARCHAR(64)  NULL COMMENT '主键值(复合/无法解析时为空)',
    operation      VARCHAR(8)   NOT NULL COMMENT 'INSERT/UPDATE/DELETE',
    affected_rows  INT          NULL COMMENT '影响行数',
    before_json    LONGTEXT     NULL COMMENT '变更前快照(超64KB截断)',
    after_json     LONGTEXT     NULL COMMENT '变更后快照(超64KB截断)',
    masked_columns VARCHAR(512) NULL COMMENT '被脱敏的列名',
    truncated      TINYINT      NOT NULL DEFAULT 0 COMMENT '快照是否被截断',
    sql_digest     VARCHAR(64)  NULL COMMENT '无法解析主键时的SQL模板摘要',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (id),
    KEY idx_change_log (audit_log_id),
    KEY idx_change_table_record (table_name, record_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='操作审计变更子表';
