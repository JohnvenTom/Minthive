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

    /** 分类 */
    private String category;

    /** 圈子简介 */
    private String intro;

    /** 圈子头像 */
    private String avatar;

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
}
