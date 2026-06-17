package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户关注实体类
 *
 * <p>对应数据库 follow 表</p>
 */
@Data
@TableName("follow")
public class Follow implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注者用户ID */
    private Long userId;

    /** 被关注用户ID */
    private Long followUserId;

    /** 关注时间 */
    private LocalDateTime createTime;
}
