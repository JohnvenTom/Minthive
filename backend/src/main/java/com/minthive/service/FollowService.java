package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Follow;
import com.minthive.entity.User;

import java.util.List;

/**
 * 关注服务接口
 */
public interface FollowService {

    /**
     * 关注用户
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     */
    void follow(Long userId, Long followUserId);

    /**
     * 取消关注
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     */
    void unfollow(Long userId, Long followUserId);

    /**
     * 查询是否已关注
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     * @return true 已关注
     */
    boolean isFollowing(Long userId, Long followUserId);

    /**
     * 获取用户的关注列表（分页）
     *
     * <p>查询指定用户关注的用户列表，关联 user 表获取用户详情</p>
     *
     * @param userId  当前用户ID
     * @param current 当前页码，默认1
     * @param size    每页条数，默认20
     * @return 分页用户列表
     */
    Page<User> getFollowingList(Long userId, long current, long size);

    /**
     * 获取用户的粉丝列表（分页）
     *
     * <p>查询关注指定用户的用户列表，关联 user 表获取用户详情，
     * 并标记当前登录用户是否已回关每个粉丝</p>
     *
     * @param userId        目标用户ID
     * @param currentUserId 当前登录用户ID（用于标记回关状态）
     * @param current       当前页码，默认1
     * @param size          每页条数，默认20
     * @return 分页用户列表
     */
    Page<User> getFollowersList(Long userId, Long currentUserId, long current, long size);

    /**
     * 推荐好友列表
     *
     * <p>简单实现：返回最近注册的10个非当前用户（后续可接入AI推荐）</p>
     *
     * @param currentUserId 当前登录用户ID（用于排除自身）
     * @return 推荐用户列表，最多10条
     */
    List<User> recommendUsers(Long currentUserId);
}
