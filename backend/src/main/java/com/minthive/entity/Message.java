package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私信消息实体类
 *
 * <p>对应数据库 message 表</p>
 */
@Data
@TableName("message")
public class Message implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送方ID */
    private Long fromUserId;

    /** 接收方ID */
    private Long toUserId;

    /** 消息内容 */
    private String content;

    /** 消息类型:0文字 1图片 2表情 */
    private Integer msgType;

    /** 已读状态:0未读 1已读 */
    private Integer isRead;

    /** AI代聊标记:0人工 1AI自动回复 */
    private Integer aiReply;

    /** 发送时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
