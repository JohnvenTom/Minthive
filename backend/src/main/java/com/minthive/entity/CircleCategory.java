package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 圈子分类实体类
 *
 * <p>对应数据库 circle_category 表</p>
 */
@Data
@TableName("circle_category")
public class CircleCategory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 排序(越小越前) */
    private Integer sort;

    /** 状态:0禁用 1启用 */
    private Integer status;
}
