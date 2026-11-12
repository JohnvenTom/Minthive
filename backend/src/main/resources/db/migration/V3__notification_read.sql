-- 通知已读记录表
-- 说明：点赞/评论/关注三类通知由业务表(make like_collect/comment/follow)实时派生，本身没有已读字段，
--       需要单独存储“用户已看过哪条通知”，否则通知会一直显示未读。
--       唯一键保证同一条通知重复标记已读时幂等。
CREATE TABLE IF NOT EXISTS notification_read (
    id        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id   BIGINT      NOT NULL COMMENT '接收通知的用户ID',
    type      VARCHAR(32) NOT NULL COMMENT '通知类型: like/comment/follow',
    ref_id    BIGINT      NOT NULL COMMENT '业务记录ID(点赞记录/评论/关注记录的主键)',
    read_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '标记已读时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_type_ref (user_id, type, ref_id),
    KEY idx_user_type (user_id, type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通知已读记录';
