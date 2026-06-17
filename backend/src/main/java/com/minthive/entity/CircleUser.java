package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 圈子成员实体类
 *
 * <p>对应数据库 circle_user 表</p>
 */
@Data
@TableName("circle_user")
public class CircleUser implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 圈子ID */
    private Long circleId;

    /** 用户ID */
    private Long userId;

    /** 成员身份:0普通成员 1圈主 */
    private Integer role;

    /** 审核状态:0待审 1通过 2拒绝 */
    private Integer auditStatus;

    /** 入圈时间 */
    private LocalDateTime joinTime;
}
