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
 * 评论实体类
 *
 * <p>对应数据库 comment 表</p>
 */
@Data
@TableName("comment")
public class Comment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 帖子ID */
    private Long postId;

    /** 评论用户ID */
    private Long userId;

    /** 父级评论ID(0为一级评论) */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 评论图片(JSON) */
    private String images;

    /** 点赞数 */
    private Integer likeCount;

    /** AI评论标记:0手动 1AI生成 */
    private Integer aiGenerated;

    /** 评论时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
