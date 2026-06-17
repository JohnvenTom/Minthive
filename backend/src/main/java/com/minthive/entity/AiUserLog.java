package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI用户行为日志实体类
 *
 * <p>对应数据库 ai_user_log 表</p>
 * <p>用于AI推荐模型持续迭代优化</p>
 */
@Data
@TableName("ai_user_log")
public class AiUserLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 浏览帖子ID */
    private Long postId;

    /** 互动类型:view/like/comment/stay */
    private String actionType;

    /** 行为时长(秒) */
    private Integer duration;

    /** 行为时间 */
    private LocalDateTime actionTime;

    /** 兴趣向量快照(JSON) */
    private String interestSnapshot;
}
