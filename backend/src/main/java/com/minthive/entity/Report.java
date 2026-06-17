package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 举报工单实体类
 *
 * <p>对应数据库 report 表</p>
 */
@Data
@TableName("report")
public class Report implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报人ID */
    private Long reporterId;

    /** 被举报内容ID */
    private Long targetId;

    /** 举报对象类型:1帖子 2评论 3私信 4用户 */
    private Integer targetType;

    /** 举报类型 */
    private String reportType;

    /** 举报原因 */
    private String reason;

    /** 工单状态:0待处理 1已驳回 2已处理 */
    private Integer status;

    /** 处理结果 */
    private String result;

    /** AI风险等级:0未评 1低 2中 3高 */
    private Integer aiRiskLevel;

    /** 举报时间 */
    private LocalDateTime createTime;

    /** 处理时间 */
    private LocalDateTime handleTime;
}
