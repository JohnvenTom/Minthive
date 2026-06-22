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
 * 兴趣圈子实体类
 *
 * <p>对应数据库 circle 表</p>
 */
@Data
@TableName("circle")
public class Circle implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 圈主用户ID */
    private Long ownerId;

    /** 圈子名称 */
    private String name;

    /** 分类ID(关联circle_category) */
    private Long categoryId;

    /** 分类名称(非数据库字段，查询时填充) */
    @TableField(exist = false)
    private String categoryName;

    /** 圈子简介 */
    private String intro;

    /** 圈子头像 */
    private String avatar;

    /** 圈子封面图 */
    private String banner;

    /** 圈子类型:0公开 1私密 */
    private Integer type;

    /** 成员数量 */
    private Integer memberCount;

    /** 圈子状态:0下架 1正常 */
    private Integer status;

    /** 是否推荐:0否 1是 */
    private Integer recommend;

    /** 圈内公告 */
    private String notice;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 当前用户是否已加入（非持久化字段，由后端查询 circle_member 表后填充） */
    @TableField(exist = false)
    private Boolean joined;

    /** 帖子数量（非持久化字段，由后端动态统计 post 表填充） */
    @TableField(exist = false)
    private Integer postCount;
}
