package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Circle;
import com.minthive.entity.CircleUser;
import com.minthive.entity.Post;
import com.minthive.mapper.CircleMapper;
import com.minthive.mapper.CircleUserMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.service.CircleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 圈子服务实现
 */
@RequiredArgsConstructor
@Service
public class CircleServiceImpl implements CircleService {

    private final CircleMapper circleMapper;

    private final CircleUserMapper circleUserMapper;

    private final PostMapper postMapper;

    /**
     * 创建圈子
     *
     * @param circle 圈子实体
     * @return 圈子实体
     */
    @Override
    @Transactional
    public Circle create(Circle circle) {
        circle.setMemberCount(1);
        circle.setStatus(1);
        circleMapper.insert(circle);
        // 圈主自动加入
        CircleUser cu = new CircleUser();
        cu.setCircleId(circle.getId());
        cu.setUserId(circle.getOwnerId());
        cu.setRole(Constants.CIRCLE_MEMBER_OWNER);
        cu.setAuditStatus(1);
        circleUserMapper.insert(cu);
        return circle;
    }

    /**
     * 根据ID查询圈子
     *
     * @param id 圈子ID
     * @return 圈子实体
     */
    @Override
    public Circle getById(Long id) {
        Circle circle = circleMapper.selectById(id);
        if (circle == null) {
            throw new BusinessException(ResultCode.CIRCLE_NOT_EXISTS);
        }
        return circle;
    }

    /**
     * 分页查询圈子
     *
     * @param current  当前页
     * @param size     每页大小
     * @param category 分类
     * @param keyword  关键词
     * @return 分页结果
     */
    @Override
    public Page<Circle> page(long current, long size, String category, String keyword) {
        LambdaQueryWrapper<Circle> wrapper = new LambdaQueryWrapper<Circle>()
                .eq(Circle::getStatus, 1)
                .eq(StringUtils.hasText(category), Circle::getCategory, category)
                .like(StringUtils.hasText(keyword), Circle::getName, keyword)
                .orderByDesc(Circle::getMemberCount);
        return circleMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 加入圈子
     *
     * @param circleId 圈子ID
     * @param userId   用户ID
     */
    @Override
    @Transactional
    public void join(Long circleId, Long userId) {
        Circle circle = getById(circleId);
        // 校验是否已加入
        Long exist = circleUserMapper.selectCount(new LambdaQueryWrapper<CircleUser>()
                .eq(CircleUser::getCircleId, circleId)
                .eq(CircleUser::getUserId, userId));
        if (exist > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_OPERATION, "已加入该圈子");
        }
        CircleUser cu = new CircleUser();
        cu.setCircleId(circleId);
        cu.setUserId(userId);
        cu.setRole(Constants.CIRCLE_MEMBER_NORMAL);
        // 公开圈子直接通过，私密圈子待审
        cu.setAuditStatus(circle.getType() == Constants.CIRCLE_TYPE_PUBLIC ? 1 : 0);
        circleUserMapper.insert(cu);
        // 成员数 +1
        circle.setMemberCount(circle.getMemberCount() + 1);
        circleMapper.updateById(circle);
    }

    /**
     * 退出圈子
     *
     * @param circleId 圈子ID
     * @param userId   用户ID
     */
    @Override
    @Transactional
    public void leave(Long circleId, Long userId) {
        circleUserMapper.delete(new LambdaQueryWrapper<CircleUser>()
                .eq(CircleUser::getCircleId, circleId)
                .eq(CircleUser::getUserId, userId));
        Circle circle = getById(circleId);
        if (circle.getMemberCount() > 0) {
            circle.setMemberCount(circle.getMemberCount() - 1);
            circleMapper.updateById(circle);
        }
    }

    /**
     * 更新圈子信息
     *
     * @param circle 圈子实体
     * @return 更新后的圈子
     */
    @Override
    public Circle update(Circle circle) {
        circleMapper.updateById(circle);
        return circleMapper.selectById(circle.getId());
    }

    /**
     * 查询圈内动态（分页）
     *
     * <p>功能描述：根据圈子ID查询该圈子内已审核通过的帖子列表，按创建时间倒序排列</p>
     *
     * @param id      圈子ID
     * @param current 当前页码，默认为1
     * @param size    每页大小，默认为10
     * @return 分页帖子结果，包含该圈内审核通过的帖子
     */
    @Override
    public Page<Post> getCirclePosts(Long id, long current, long size) {
        // 校验圈子是否存在
        getById(id);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getCircleId, id)
                .eq(Post::getAuditStatus, 1)
                .orderByDesc(Post::getCreateTime);
        return postMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 获取圈子分类列表
     *
     * <p>功能描述：返回系统预设的圈子分类名称列表</p>
     *
     * @return 预设分类名称列表，包含：技术、生活、娱乐、学习、运动、美食、旅行、游戏、音乐、其他
     */
    @Override
    public List<String> getCategories() {
        return Arrays.asList("技术", "生活", "娱乐", "学习", "运动", "美食", "旅行", "游戏", "音乐", "其他");
    }

    /**
     * 获取推荐圈子列表
     *
     * <p>功能描述：返回成员数最多的前10个推荐圈子，用于首页推荐展示</p>
     *
     * @return 推荐圈子列表，最多返回10条记录
     */
    @Override
    public List<Circle> recommendCircles() {
        LambdaQueryWrapper<Circle> wrapper = new LambdaQueryWrapper<Circle>()
                .eq(Circle::getStatus, 1)
                .orderByDesc(Circle::getMemberCount)
                .last("LIMIT 10");
        return circleMapper.selectList(wrapper);
    }
}
