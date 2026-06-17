package com.minthive.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录用户信息
 *
 * <p>功能描述：封装当前线程持有的登录用户基础信息</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 账号 */
    private String account;

    /** 角色: 0普通 1圈主 2管理员 */
    private Integer role;
}
