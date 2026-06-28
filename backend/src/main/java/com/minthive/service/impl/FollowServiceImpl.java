package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.entity.Follow;
import com.minthive.entity.User;
import com.minthive.mapper.FollowMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.service.FollowService;
import com.minthive.service.InterestVectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关注服务实现
 */
@RequiredArgsConstructor
@Service
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final InterestVectorService interestVectorService;

    /**
     * 关注用户
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     * @throws BusinessException 重复关注时抛出
     */
    @Override
    public void follow(Long userId, Long followUserId) {
        if (userId.equals(followUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能关注自己");
        }
        Long exist = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId));
        if (exist > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_OPERATION, "已关注该用户");
        }
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        followMapper.insert(follow);
    }

    /**
     * 取消关注
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     */
    @Override
    public void unfollow(Long userId, Long followUserId) {
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId));
    }

    /**
     * 查询是否已关注
     *
     * @param userId       关注者ID
     * @param followUserId 被关注者ID
     * @return true 已关注
     */
    @Override
    public boolean isFollowing(Long userId, Long followUserId) {
        Long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId));
        return count > 0;
    }

    /**
     * 获取用户的关注列表（分页）
     *
     * <p>查询 follow 表中 userId 作为关注者的记录，提取被关注者ID列表，
     * 再从 user 表查询对应的用户详情信息</p>
     *
     * @param userId  当前用户ID
     * @param current 当前页码，默认1
     * @param size    每页条数，默认20
     * @return 分页用户列表（包含被关注用户的详细信息）
     */
    @Override
    public Page<User> getFollowingList(Long userId, long current, long size) {
        // 1. 分页查询关注记录
        Page<Follow> followPage = followMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getUserId, userId)
                        .orderByDesc(Follow::getCreateTime)
        );
        if (followPage.getRecords().isEmpty()) {
            return new Page<>(current, size);
        }
        // 2. 提取被关注用户ID列表
        List<Long> followUserIds = followPage.getRecords()
                .stream()
                .map(Follow::getFollowUserId)
                .collect(Collectors.toList());
        // 3. 查询用户详情
        List<User> users = userMapper.selectBatchIds(followUserIds);
        // 4. 组装分页结果
        Page<User> userPage = new Page<>(current, size, followPage.getTotal());
        userPage.setRecords(users);
        return userPage;
    }

    /**
     * 获取用户的粉丝列表（分页）
     *
     * <p>查询 follow 表中 userId 作为被关注者的记录，提取关注者ID列表，
     * 再从 user 表查询对应的用户详情信息，同时标记当前登录用户是否已回关</p>
     *
     * @param userId        目标用户ID
     * @param currentUserId 当前登录用户ID（用于标记回关状态）
     * @param current       当前页码，默认1
     * @param size          每页条数，默认20
     * @return 分页用户列表（包含粉丝的详细信息）
     */
    @Override
    public Page<User> getFollowersList(Long userId, Long currentUserId, long current, long size) {
        // 1. 分页查询粉丝记录
        Page<Follow> followPage = followMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowUserId, userId)
                        .orderByDesc(Follow::getCreateTime)
        );
        if (followPage.getRecords().isEmpty()) {
            return new Page<>(current, size);
        }
        // 2. 提取关注者ID列表
        List<Long> followerIds = followPage.getRecords()
                .stream()
                .map(Follow::getUserId)
                .collect(Collectors.toList());
        // 3. 查询用户详情
        List<User> users = userMapper.selectBatchIds(followerIds);
        // 4. 标记当前登录用户是否已回关每个粉丝
        if (currentUserId != null && !currentUserId.equals(userId)) {
            List<Long> followedIds = followMapper.selectList(
                    new LambdaQueryWrapper<Follow>()
                            .eq(Follow::getUserId, currentUserId)
                            .in(Follow::getFollowUserId, followerIds)
            ).stream().map(Follow::getFollowUserId).collect(Collectors.toList());
            Set<Long> followedSet = new HashSet<>(followedIds);
            for (User u : users) {
                u.setIsFollowing(followedSet.contains(u.getId()));
            }
        }
        // 5. 组装分页结果
        Page<User> userPage = new Page<>(current, size, followPage.getTotal());
        userPage.setRecords(users);
        return userPage;
    }

    /**
     * 推荐好友列表
     *
     * <p>有行为数据时，推荐兴趣向量余弦相似度最高的用户；否则返回最近注册的用户</p>
     *
     * @param currentUserId 当前登录用户ID（用于排除自身）
     * @return 推荐用户列表，最多10条
     */
    @Override
    public List<User> recommendUsers(Long currentUserId) {
        Map<String, Double> myVector = interestVectorService.getVector(currentUserId);
        if (myVector.isEmpty()) {
            return userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .ne(User::getId, currentUserId)
                            .orderByDesc(User::getRegisterTime)
                            .last("LIMIT 10")
            );
        }

        // 取有 aiInterestVector 的其他用户
        List<User> candidates = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .ne(User::getId, currentUserId)
                        .isNotNull(User::getAiInterestVector)
                        .ne(User::getAiInterestVector, "")
                        .ne(User::getAiInterestVector, "{}")
                        .last("LIMIT 50")
        );

        if (candidates.isEmpty()) {
            return userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .ne(User::getId, currentUserId)
                            .orderByDesc(User::getRegisterTime)
                            .last("LIMIT 10")
            );
        }

        List<User> scored = new ArrayList<>();
        Map<Long, Double> simMap = new HashMap<>();
        for (User u : candidates) {
            Map<String, Double> otherVector = interestVectorService.getVector(u.getId());
            if (!otherVector.isEmpty()) {
                double sim = cosineSimilarity(myVector, otherVector);
                simMap.put(u.getId(), sim);
                scored.add(u);
            }
        }

        scored.sort((a, b) -> Double.compare(simMap.getOrDefault(b.getId(), 0.0), simMap.getOrDefault(a.getId(), 0.0)));
        return scored.size() > 10 ? scored.subList(0, 10) : scored;
    }

    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        Set<String> keys = new HashSet<>(v1.keySet());
        keys.retainAll(v2.keySet());
        if (keys.isEmpty()) return 0;
        double dot = 0, n1 = 0, n2 = 0;
        for (String k : keys) {
            double a = v1.getOrDefault(k, 0.0);
            double b = v2.getOrDefault(k, 0.0);
            dot += a * b;
        }
        for (double v : v1.values()) n1 += v * v;
        for (double v : v2.values()) n2 += v * v;
        if (n1 == 0 || n2 == 0) return 0;
        return dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }
}
