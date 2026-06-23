package com.minthive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * <p>对应数据库 user 表</p>
 */
@Data
@TableName("user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号 */
    private String account;

    /** 密码(Bcrypt加密) */
    private String password;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 昵称 */
    private String nickname;

    /** 个人简介 */
    private String bio;

    /** 性别:0未知 1男 2女 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 兴趣标签(逗号分隔) */
    private String interestTags;

    /** AI用户兴趣向量 */
    private String aiInterestVector;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 最近登录时间（每次成功登录时更新） */
    private LocalDateTime lastLoginTime;

    /** 账号状态:0封禁 1正常 */
    private Integer status;

    /** 注销状态:0正常 1注销中 2已注销 */
    private Integer cancelStatus;

    /** 角色:0普通 1圈主 2管理员 */
    private Integer role;

    /**
     * 帖子数（非持久化字段）
     * @description 不对应数据库列，由后端从 post 表实时统计后填充返回给前端
     */
    @TableField(exist = false)
    private Integer postCount;

    /**
     * 关注数（非持久化字段）
     * @description 不对应数据库列，由后端从 follow 表实时统计（follow_user_id=当前用户）后填充
     */
    @TableField(exist = false)
    private Integer followCount;

    /**
     * 粉丝数（非持久化字段）
     * @description 不对应数据库列，由后端从 follow 表实时统计（user_id=当前用户）后填充
     */
    @TableField(exist = false)
    private Integer fanCount;

    /**
     * 当前登录用户是否已关注该用户（非持久化字段）
     * @description 不对应数据库列，由后端从 follow 表查询后填充返回给前端
     */
    @TableField(exist = false)
    private Boolean isFollowing;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
