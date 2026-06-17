package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.common.RedisConstants;
import com.minthive.entity.User;
import com.minthive.mapper.UserMapper;
import com.minthive.security.JwtUtils;
import com.minthive.service.UserService;
import com.minthive.util.BcryptUtil;
import com.minthive.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 *
 * <p>功能描述：用户注册、登录、信息维护等业务实现</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final JwtUtils jwtUtils;

    private final RedisUtil redisUtil;

    /**
     * 用户注册
     *
     * @param account  账号
     * @param password 明文密码
     * @param phone    手机号
     * @return 用户实体
     * @throws BusinessException 账号已存在时抛出
     */
    @Override
    public User register(String account, String password, String phone) {
        // 校验账号唯一
        Long existCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getAccount, account));
        if (existCount > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }
        User user = new User();
        user.setAccount(account);
        user.setPassword(BcryptUtil.encode(password));
        user.setPhone(phone);
        user.setNickname("用户" + account);
        user.setStatus(Constants.USER_STATUS_NORMAL);
        user.setCancelStatus(Constants.CANCEL_STATUS_NORMAL);
        user.setRole(Constants.ROLE_USER);
        user.setRegisterTime(LocalDateTime.now());
        userMapper.insert(user);
        log.info("用户注册成功: account={}, id={}", account, user.getId());
        return user;
    }

    /**
     * 用户登录
     *
     * @param account  账号
     * @param password 明文密码
     * @return JWT Token
     * @throws BusinessException 账号不存在/密码错误/账号封禁时抛出
     */
    @Override
    public String login(String account, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getAccount, account));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }
        if (Constants.USER_STATUS_BANNED == user.getStatus()) {
            throw new BusinessException(ResultCode.USER_BANNED);
        }
        if (!BcryptUtil.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        String token = jwtUtils.generateToken(user.getId(), user.getAccount());
        // 缓存登录态
        redisUtil.cacheLoginToken(user.getId(), token);
        log.info("用户登录成功: account={}, id={}", account, user.getId());
        return token;
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户实体
     */
    @Override
    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 更新用户信息
     *
     * @param user 用户实体
     * @return 更新后的用户
     */
    @Override
    public User update(User user) {
        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    /**
     * 修改密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @throws BusinessException 旧密码错误时抛出
     */
    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }
        if (!BcryptUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(BcryptUtil.encode(newPassword));
        userMapper.updateById(update);
        // 清除登录态，强制重新登录
        redisUtil.deleteLoginToken(userId);
    }

    /**
     * 注销账号(进入冷静期)
     *
     * @param userId 用户ID
     */
    @Override
    public void cancelAccount(Long userId) {
        User update = new User();
        update.setId(userId);
        update.setCancelStatus(Constants.CANCEL_STATUS_PENDING);
        userMapper.updateById(update);
        redisUtil.deleteLoginToken(userId);
        log.info("用户 {} 申请注销，进入冷静期", userId);
    }

    /**
     * 按关键词搜索用户（昵称/账号模糊匹配）
     *
     * @param keyword 搜索关键词(可空，为空时返回全部)
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果（密码字段已脱敏）
     */
    @Override
    public Page<User> searchByKeyword(String keyword, long current, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(User::getNickname, keyword)
                    .or()
                    .like(User::getAccount, keyword);
        }
        wrapper.orderByDesc(User::getRegisterTime);
        Page<User> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        // 脱敏：清除密码字段
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }
}
