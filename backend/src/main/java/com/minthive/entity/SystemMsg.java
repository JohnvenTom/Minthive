package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统消息实体类
 *
 * <p>对应数据库 system_msg 表</p>
 */
@Data
@TableName("system_msg")
public class SystemMsg implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** 消息类型:1点赞 2评论 3私信 4圈子公告 5系统公告 */
    private Integer msgType;

    /** 消息内容 */
    private String content;

    /** 关联目标ID(如帖子ID) */
    private Long targetId;

    /** 是否已读:0未读 1已读 */
    private Integer isRead;

    /** 推送时间 */
    private LocalDateTime createTime;
}
