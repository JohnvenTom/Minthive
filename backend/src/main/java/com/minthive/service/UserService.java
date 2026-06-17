package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.User;

/**
 * 用户服务接口
 *
 * <p>功能描述：定义用户注册、登录、信息维护等业务方法</p>
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param account  账号
     * @param password 明文密码
     * @param phone    手机号(可空)
     * @return 注册成功的用户实体
     */
    User register(String account, String password, String phone);

    /**
     * 用户登录
     *
     * @param account  账号
     * @param password 明文密码
     * @return JWT Token
     */
    String login(String account, String password);

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户实体（密码字段为 null）
     */
    User getById(Long userId);

    /**
     * 根据账号查询用户信息
     *
     * @param account 账号
     * @return 用户实体（密码字段为 null）
     */
    User getByAccount(String account);

    /**
     * 更新用户信息
     *
     * @param user 用户实体
     * @return 更新后的用户
     */
    User update(User user);

    /**
     * 修改密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 注销账号(进入冷静期)
     *
     * @param userId 用户ID
     */
    void cancelAccount(Long userId);

    /**
     * 按关键词搜索用户（昵称/账号模糊匹配）
     *
     * @param keyword 搜索关键词(可空)
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    Page<User> searchByKeyword(String keyword, long current, long size);
}
