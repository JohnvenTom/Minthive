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

    /** 账号状态:0封禁 1正常 */
    private Integer status;

    /** 注销状态:0正常 1注销中 2已注销 */
    private Integer cancelStatus;

    /** 角色:0普通 1圈主 2管理员 */
    private Integer role;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
