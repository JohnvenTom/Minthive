package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知已读记录实体类
 *
 * <p>对应数据库 notification_read 表。点赞/评论/关注通知由业务表实时派生，
 * 该表记录“某用户已读某条通知”，用于回填已读状态与统计未读数</p>
 */
@Data
@TableName("notification_read")
public class NotificationRead implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收通知的用户ID */
    private Long userId;

    /** 通知类型: like(点赞) / comment(评论) / follow(关注) */
    private String type;

    /** 业务记录ID：点赞记录ID、评论ID或关注记录ID */
    private Long refId;

    /** 标记已读时间 */
    private LocalDateTime readTime;
}
