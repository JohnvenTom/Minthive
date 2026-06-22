package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统公告实体类
 *
 * <p>对应数据库 announcement 表，用于存储管理员发布的平台级公告</p>
 */
@Data
@TableName("announcement")
public class Announcement implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 状态:0禁用 1启用 */
    private Integer status;

    /** 发布时间 */
    private LocalDateTime publishTime;
}
