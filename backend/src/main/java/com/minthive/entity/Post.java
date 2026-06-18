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
 * 动态帖子实体类
 *
 * <p>对应数据库 post 表</p>
 */
@Data
@TableName("post")
public class Post implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布用户ID */
    private Long userId;

    /** 文案内容 */
    private String content;

    /** 图片地址(JSON数组) */
    private String images;

    /** 视频地址 */
    private String video;

    /** 可见范围:0公开 1仅粉丝 2仅自己 */
    private Integer visibility;

    /** 审核状态:0待审 1通过 2驳回 */
    private Integer auditStatus;

    /**
     * 点赞数（非持久化字段）
     * @description 不对应数据库列，由后端从 like_collect 表实时统计后填充返回给前端
     */
    @TableField(exist = false)
    private Integer likeCount;

    /**
     * 评论数（非持久化字段）
     * @description 不对应数据库列，由后端从 comment 表实时统计后填充返回给前端
     */
    @TableField(exist = false)
    private Integer commentCount;

    /**
     * 收藏数（非持久化字段）
     * @description 不对应数据库列，由后端从 like_collect 表实时统计后填充返回给前端
     */
    @TableField(exist = false)
    private Integer collectCount;

    /** 圈子ID */
    private Long circleId;

    /** AI文案生成标记:0手动 1AI生成 */
    private Integer aiGenerated;

    /** 话题标签 */
    private String tags;

    /** 转发原帖ID(非空时表示转发帖) */
    private Long sharePostId;

    /** 发布时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
