-- ============================================================
-- MintHive 兴趣社交平台 数据库初始化脚本
-- 数据库: MySQL 8.0
-- 字符集: utf8mb4
-- 存储引擎: InnoDB
-- 共计 12 张核心表
-- ============================================================

CREATE DATABASE IF NOT EXISTS `minthive` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `minthive`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 用户表 user
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `account`             VARCHAR(64)  NOT NULL COMMENT '账号(手机号/邮箱/自定义)',
    `password`            VARCHAR(100) NOT NULL COMMENT '密码(Bcrypt加密)',
    `phone`               VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`              VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `nickname`            VARCHAR(64)  NOT NULL COMMENT '昵称',
    `bio`                 VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    `gender`              TINYINT      DEFAULT 0 COMMENT '性别:0未知 1男 2女',
    `birthday`            DATE         DEFAULT NULL COMMENT '生日',
    `interest_tags`       VARCHAR(500) DEFAULT NULL COMMENT '兴趣标签(逗号分隔)',
    `ai_interest_vector`  TEXT         DEFAULT NULL COMMENT 'AI用户兴趣向量(JSON/向量字符串)',
    `register_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `status`              TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态:0封禁 1正常',
    `cancel_status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '注销状态:0正常 1注销中 2已注销',
    `role`                TINYINT      NOT NULL DEFAULT 0 COMMENT '角色:0普通用户 1圈主 2管理员',
    `deleted`             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account` (`account`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 用户关注表 follow
-- ============================================================
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT   NOT NULL COMMENT '关注者用户ID',
    `follow_user_id` BIGINT  NOT NULL COMMENT '被关注用户ID',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`),
    KEY `idx_follow_user` (`follow_user_id`),
    CONSTRAINT `fk_follow_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_follow_target` FOREIGN KEY (`follow_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- ============================================================
-- 3. 动态帖子表 post
-- ============================================================
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
    `user_id`        BIGINT       NOT NULL COMMENT '发布用户ID',
    `content`        TEXT         NOT NULL COMMENT '文案内容',
    `images`         VARCHAR(2000) DEFAULT NULL COMMENT '图片地址(JSON数组)',
    `video`          VARCHAR(255) DEFAULT NULL COMMENT '视频地址',
    `visibility`     TINYINT      NOT NULL DEFAULT 0 COMMENT '可见范围:0公开 1仅粉丝 2仅自己',
    `audit_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态:0待审 1通过 2驳回',
    `circle_id`      BIGINT       DEFAULT NULL COMMENT '圈子ID(可空)',
    `ai_generated`   TINYINT      NOT NULL DEFAULT 0 COMMENT 'AI文案生成标记:0手动 1AI生成',
    `tags`           VARCHAR(255) DEFAULT NULL COMMENT '话题标签(逗号分隔)',
    `share_post_id`  BIGINT       DEFAULT NULL COMMENT '转发原帖ID(非空时表示转发帖)',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_circle` (`circle_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_audit` (`audit_status`),
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态帖子表';

-- ============================================================
-- 4. 评论表 comment
-- ============================================================
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `post_id`       BIGINT       NOT NULL COMMENT '帖子ID',
    `user_id`       BIGINT       NOT NULL COMMENT '评论用户ID',
    `parent_id`     BIGINT       DEFAULT 0 COMMENT '父级评论ID(0为一级评论)',
    `reply_to_id`   BIGINT       DEFAULT NULL COMMENT '回复目标用户ID',
    `content`       VARCHAR(1000) NOT NULL COMMENT '评论内容',
    `images`        VARCHAR(2000) DEFAULT NULL COMMENT '评论图片(JSON)',
    `ai_generated`  TINYINT      NOT NULL DEFAULT 0 COMMENT 'AI评论标记:0手动 1AI生成',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_post` (`post_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_parent` (`parent_id`),
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ============================================================
-- 5. 点赞收藏表 like_collect
-- ============================================================
DROP TABLE IF EXISTS `like_collect`;
CREATE TABLE `like_collect` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `target_id`    BIGINT   NOT NULL COMMENT '关联内容ID(帖子/评论ID)',
    `user_id`      BIGINT   NOT NULL COMMENT '用户ID',
    `type`         TINYINT  NOT NULL COMMENT '类型:1点赞帖子 2点赞评论 3收藏帖子',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target_type` (`user_id`, `target_id`, `type`),
    KEY `idx_target_type` (`target_id`, `type`),
    CONSTRAINT `fk_lc_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞收藏表';

-- ============================================================
-- 6. 圈子分类表 circle_category
-- ============================================================
DROP TABLE IF EXISTS `circle_category`;
CREATE TABLE `circle_category` (
    `id`       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`     VARCHAR(32) NOT NULL COMMENT '分类名称',
    `sort`     INT         NOT NULL DEFAULT 0 COMMENT '排序(越小越前)',
    `status`   TINYINT     NOT NULL DEFAULT 1 COMMENT '状态:0禁用 1启用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='圈子分类表';

-- ============================================================
-- 7. 兴趣圈子表 circle
-- ============================================================
DROP TABLE IF EXISTS `circle`;
CREATE TABLE `circle` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '圈子ID',
    `owner_id`     BIGINT       NOT NULL COMMENT '圈主用户ID',
    `name`         VARCHAR(64)  NOT NULL COMMENT '圈子名称',
    `category_id`  BIGINT       NOT NULL COMMENT '分类ID(关联circle_category)',
    `intro`        VARCHAR(500) DEFAULT NULL COMMENT '圈子简介',
    `avatar`       VARCHAR(255) DEFAULT NULL COMMENT '圈子头像',
    `type`         TINYINT      NOT NULL DEFAULT 0 COMMENT '圈子类型:0公开 1私密',
    `member_count` INT          NOT NULL DEFAULT 0 COMMENT '成员数量',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '圈子状态:0下架 1正常',
    `recommend`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否推荐:0否 1是',
    `notice`       VARCHAR(1000) DEFAULT NULL COMMENT '圈内公告',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_category_id` (`category_id`),
    CONSTRAINT `fk_circle_owner` FOREIGN KEY (`owner_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_circle_category` FOREIGN KEY (`category_id`) REFERENCES `circle_category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兴趣圈子表';

-- ============================================================
-- 8. 圈子成员表 circle_user
-- ============================================================
DROP TABLE IF EXISTS `circle_user`;
CREATE TABLE `circle_user` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `circle_id`    BIGINT   NOT NULL COMMENT '圈子ID',
    `user_id`      BIGINT   NOT NULL COMMENT '用户ID',
    `role`         TINYINT  NOT NULL DEFAULT 0 COMMENT '成员身份:0普通成员 1圈主',
    `audit_status` TINYINT  NOT NULL DEFAULT 1 COMMENT '审核状态:0待审 1通过 2拒绝',
    `join_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入圈时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_circle_user` (`circle_id`, `user_id`),
    KEY `idx_user` (`user_id`),
    CONSTRAINT `fk_cu_circle` FOREIGN KEY (`circle_id`) REFERENCES `circle`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cu_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='圈子成员表';

-- ============================================================
-- 8. 私信消息表 message
-- ============================================================
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `from_user_id` BIGINT       NOT NULL COMMENT '发送方ID',
    `to_user_id`   BIGINT       NOT NULL COMMENT '接收方ID',
    `content`      VARCHAR(1000) NOT NULL COMMENT '消息内容',
    `msg_type`     TINYINT      NOT NULL DEFAULT 0 COMMENT '消息类型:0文字 1图片 2表情',
    `is_read`      TINYINT      NOT NULL DEFAULT 0 COMMENT '已读状态:0未读 1已读',
    `ai_reply`     TINYINT      NOT NULL DEFAULT 0 COMMENT 'AI代聊标记:0人工 1AI自动回复',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_from_to` (`from_user_id`, `to_user_id`),
    KEY `idx_to_read` (`to_user_id`, `is_read`),
    CONSTRAINT `fk_msg_from` FOREIGN KEY (`from_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_msg_to` FOREIGN KEY (`to_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信消息表';

-- ============================================================
-- 9. 举报工单表 report
-- ============================================================
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工单ID',
    `reporter_id`   BIGINT       NOT NULL COMMENT '举报人ID',
    `target_id`     BIGINT       NOT NULL COMMENT '被举报内容ID',
    `target_type`   TINYINT      NOT NULL COMMENT '举报对象类型:1帖子 2评论 3私信 4用户',
    `report_type`   VARCHAR(32)  NOT NULL COMMENT '举报类型:低俗色情/广告引流/人身攻击/违法内容/抄袭搬运',
    `reason`        VARCHAR(500) DEFAULT NULL COMMENT '举报原因',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '工单状态:0待处理 1已驳回 2已处理',
    `result`        VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    `ai_risk_level` TINYINT      DEFAULT 0 COMMENT 'AI风险等级:0未评 1低 2中 3高',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    `handle_time`   DATETIME     DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_reporter` (`reporter_id`),
    KEY `idx_status` (`status`),
    KEY `idx_risk` (`ai_risk_level`),
    CONSTRAINT `fk_report_user` FOREIGN KEY (`reporter_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报工单表';

-- ============================================================
-- 10. 系统消息表 system_msg
-- ============================================================
DROP TABLE IF EXISTS `system_msg`;
CREATE TABLE `system_msg` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id`     BIGINT       NOT NULL COMMENT '接收用户ID',
    `msg_type`    TINYINT      NOT NULL COMMENT '消息类型:1点赞 2评论 3私信 4圈子公告 5系统公告 6回复',
    `content`     VARCHAR(500) NOT NULL COMMENT '消息内容',
    `target_id`   BIGINT       DEFAULT NULL COMMENT '关联目标ID(如帖子ID)',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读:0未读 1已读',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推送时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    CONSTRAINT `fk_sysmsg_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统消息表';

-- ============================================================
-- 11. AI用户行为日志表 ai_user_log (新增专属表)
-- ============================================================
DROP TABLE IF EXISTS `ai_user_log`;
CREATE TABLE `ai_user_log` (
    `id`                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`           BIGINT   NOT NULL COMMENT '用户ID',
    `post_id`           BIGINT   DEFAULT NULL COMMENT '浏览帖子ID',
    `action_type`       VARCHAR(32) NOT NULL COMMENT '互动类型:view/like/comment/stay',
    `duration`          INT      DEFAULT 0 COMMENT '行为时长(秒)',
    `action_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
    `interest_snapshot` TEXT     DEFAULT NULL COMMENT '兴趣向量快照(JSON)',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_post` (`post_id`),
    KEY `idx_action_time` (`action_time`),
    CONSTRAINT `fk_ailog_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI用户行为日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始数据
-- ============================================================

-- 管理员账号 (密码: admin123, Bcrypt加密, strength=10)
INSERT INTO `user` (`account`, `password`, `phone`, `nickname`, `bio`, `role`, `status`)
VALUES ('admin', '$2b$10$sO9SaS7A8UOiOpPFojpBzOgQkstUlUbkY08rZsG83otSfNwrtIAxS', '13800000000', '平台管理员', 'MintHive平台超级管理员', 2, 1);

-- 圈子分类初始数据
INSERT INTO `circle_category` (`name`, `sort`, `status`)
VALUES
('技术',   1, 1),
('生活',   2, 1),
('娱乐',   3, 1),
('学习',   4, 1),
('运动',   5, 1),
('美食',   6, 1),
('旅行',   7, 1),
('游戏',   8, 1),
('音乐',   9, 1),
('其他',  10, 1);

-- 默认圈子数据 (category_id 对应上面的分类ID)
INSERT INTO `circle` (`owner_id`, `name`, `category_id`, `intro`, `type`, `member_count`, `status`, `notice`)
VALUES
(1, '游戏玩家联盟', 8, '聚集各类游戏爱好者，分享游戏心得与攻略', 0, 1, 1, '欢迎来到游戏玩家联盟！'),
(1, '影视爱好者', 3, '电影、电视剧、动漫讨论交流圈', 0, 1, 1, '欢迎来到影视爱好者圈子！'),
(1, '户外露营圈', 7, '露营、徒步、骑行等户外活动分享', 0, 1, 1, '欢迎来到户外露营圈！'),
(1, '读书分享会', 4, '读书笔记、好书推荐、文学讨论', 0, 1, 1, '欢迎来到读书分享会！'),
(1, '摄影作品交流', 10, '摄影作品分享、技巧交流、器材讨论', 0, 1, 1, '欢迎来到摄影作品交流圈！');

-- 圈主默认加入自己的圈子
INSERT INTO `circle_user` (`circle_id`, `user_id`, `role`, `audit_status`)
VALUES
(1, 1, 1, 1),
(2, 1, 1, 1),
(3, 1, 1, 1),
(4, 1, 1, 1),
(5, 1, 1, 1);

-- 敏感词初始数据(示例)
-- 完整敏感词库由 sensitive-words.txt 文件加载，此处仅作示例
