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
import com.minthive.service.SmsService;
import com.minthive.util.BcryptUtil;
import com.minthive.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    private final SmsService smsService;

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
     * @return 用户实体（密码字段已脱敏）
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
     * 根据账号查询用户信息
     *
     * @param account 账号
     * @return 用户实体（密码字段已脱敏）
     * @throws BusinessException 账号不存在时抛出
     */
    @Override
    public User getByAccount(String account) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getAccount, account));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 根据手机号查询用户信息
     *
     * @param phone 手机号
     * @return 用户实体（密码字段已脱敏）
     * @throws BusinessException 用户不存在时抛出
     */
    @Override
    public User getByPhone(String phone) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
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

    /**
     * 手机号验证码登录
     *
     * <p>流程：校验验证码 → 根据手机号查找用户 → 生成JWT Token</p>
     *
     * @param phone 手机号
     * @param code  短信验证码
     * @return JWT Token
     * @throws BusinessException 验证码错误或用户不存在时抛出
     */
    @Override
    public String loginBySms(String phone, String code) {
        // 校验验证码
        if (!smsService.verifyCode(phone, code)) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        // 根据手机号查找用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }
        if (Constants.USER_STATUS_BANNED == user.getStatus()) {
            throw new BusinessException(ResultCode.USER_BANNED);
        }
        String token = jwtUtils.generateToken(user.getId(), user.getAccount());
        redisUtil.cacheLoginToken(user.getId(), token);
        log.info("用户验证码登录成功: phone={}, id={}", phone, user.getId());
        return token;
    }

    /**
     * 带验证码的用户注册
     *
     * <p>流程：校验验证码 → 校验账号唯一性 → 创建用户</p>
     *
     * @param account  账号
     * @param password 明文密码
     * @param phone    手机号
     * @param code     短信验证码
     * @return 注册成功的用户实体
     * @throws BusinessException 验证码错误或账号已存在时抛出
     */
    @Override
    public User registerWithCode(String account, String password, String phone, String code) {
        // 校验验证码
        if (!smsService.verifyCode(phone, code)) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        return register(account, password, phone);
    }

    /**
     * 重置密码（通过验证码校验身份）
     *
     * <p>流程：校验验证码 → 根据手机号查找用户 → 更新密码 → 清除登录态</p>
     *
     * @param phone       手机号
     * @param code        短信验证码
     * @param newPassword 新密码
     * @throws BusinessException 验证码错误或用户不存在时抛出
     */
    @Override
    public void resetPassword(String phone, String code, String newPassword) {
        // 校验验证码
        if (!smsService.verifyCode(phone, code)) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        // 根据手机号查找用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }
        // 更新密码
        User update = new User();
        update.setId(user.getId());
        update.setPassword(BcryptUtil.encode(newPassword));
        userMapper.updateById(update);
        // 清除登录态，强制重新登录
        redisUtil.deleteLoginToken(user.getId());
        log.info("用户重置密码成功: phone={}, id={}", phone, user.getId());
    }

    /**
     * 更新用户兴趣标签
     *
     * <p>将兴趣标签列表转为逗号分隔字符串存入数据库</p>
     *
     * @param userId    用户ID
     * @param interests 兴趣标签列表
     */
    @Override
    public void updateInterests(Long userId, List<String> interests) {
        User update = new User();
        update.setId(userId);
        update.setInterestTags(String.join(",", interests));
        userMapper.updateById(update);
        log.info("用户兴趣标签更新: userId={}, tags={}", userId, update.getInterestTags());
    }
}
