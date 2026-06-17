package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 点赞收藏实体类
 *
 * <p>对应数据库 like_collect 表</p>
 */
@Data
@TableName("like_collect")
public class LikeCollect implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联内容ID(帖子/评论ID) */
    private Long targetId;

    /** 用户ID */
    private Long userId;

    /** 类型:1点赞帖子 2点赞评论 3收藏帖子 */
    private Integer type;

    /** 创建时间 */
    private LocalDateTime createTime;
}
