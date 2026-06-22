package com.minthive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.Constants;
import com.minthive.common.ResultCode;
import com.minthive.entity.Circle;
import com.minthive.entity.CircleCategory;
import com.minthive.entity.CircleUser;
import com.minthive.entity.Post;
import com.minthive.mapper.CircleCategoryMapper;
import com.minthive.mapper.CircleMapper;
import com.minthive.mapper.CircleMemberMapper;
import com.minthive.mapper.CircleUserMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.service.CircleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 圈子服务实现
 */
@RequiredArgsConstructor
@Service
public class CircleServiceImpl implements CircleService {

    private final CircleMapper circleMapper;

    private final CircleUserMapper circleUserMapper;

    private final CircleMemberMapper circleMemberMapper;

    private final PostMapper postMapper;

    private final CircleCategoryMapper circleCategoryMapper;

    /**
     * 创建圈子
     * <p>支持传入 categoryId（预设分类）或自动创建自定义分类</p>
     *
     * @param circle 圈子实体（categoryId 为分类ID）
     * @return 圈子实体
     */
    @Override
    @Transactional
    public Circle create(Circle circle) {
        // 确保有分类ID：如果未传 categoryId，尝试按名称匹配或新建
        resolveCategoryId(circle);
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
     * 解析/补全圈子分类ID
     * <p>如果 categoryId 已有值则直接使用；否则忽略（兼容旧逻辑）</p>
     */
    private void resolveCategoryId(Circle circle) {
        if (circle.getCategoryId() != null && circle.getCategoryId() > 0) {
            return;
        }
        throw new BusinessException(ResultCode.PARAM_ERROR, "请选择圈子分类");
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
        fillCategoryName(List.of(circle));
        return circle;
    }

    /**
     * 分页查询圈子
     *
     * @param current    当前页
     * @param size       每页大小
     * @param categoryId 分类ID
     * @param keyword    关键词
     * @return 分页结果
     */
    @Override
    public Page<Circle> page(long current, long size, Long categoryId, String keyword) {
        LambdaQueryWrapper<Circle> wrapper = new LambdaQueryWrapper<Circle>()
                .eq(Circle::getStatus, 1)
                .eq(categoryId != null && categoryId > 0, Circle::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Circle::getName, keyword)
                .orderByDesc(Circle::getMemberCount);
        Page<Circle> result = circleMapper.selectPage(new Page<>(current, size), wrapper);
        fillCategoryName(result.getRecords());
        return result;
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
     * <p>功能描述：从数据库查询所有启用的圈子分类，按排序字段升序排列</p>
     *
     * @return 启用的分类列表
     */
    @Override
    public List<CircleCategory> getCategories() {
        return circleCategoryMapper.selectList(
                new LambdaQueryWrapper<CircleCategory>()
                        .eq(CircleCategory::getStatus, 1)
                        .orderByAsc(CircleCategory::getSort)
        );
    }

    /**
     * 创建自定义分类并返回ID
     *
     * @param name 分类名称
     * @return 新建分类的ID
     */
    @Override
    @Transactional
    public Long createCategory(String name) {
        // 先查是否已存在同名分类
        CircleCategory exist = circleCategoryMapper.selectOne(
                new LambdaQueryWrapper<CircleCategory>()
                        .eq(CircleCategory::getName, name)
                        .last("LIMIT 1")
        );
        if (exist != null) {
            return exist.getId();
        }
        // 获取当前最大排序值
        Long maxSort = circleCategoryMapper.selectCount(
                new LambdaQueryWrapper<CircleCategory>()
        );
        CircleCategory category = new CircleCategory();
        category.setName(name);
        category.setSort(maxSort.intValue() + 1);
        category.setStatus(1);
        circleCategoryMapper.insert(category);
        return category.getId();
    }

    /**
     * 批量填充圈子的分类名称
     */
    private void fillCategoryName(List<Circle> circles) {
        if (circles == null || circles.isEmpty()) return;
        // 收集所有 categoryId
        List<Long> ids = circles.stream()
                .map(Circle::getCategoryId)
                .distinct()
                .toList();
        if (ids.isEmpty()) return;
        // 批量查询分类名称
        Map<Long, String> nameMap = circleCategoryMapper.selectBatchIds(ids)
                .stream()
                .collect(Collectors.toMap(CircleCategory::getId, CircleCategory::getName, (a, b) -> a));
        // 填充
        circles.forEach(c -> c.setCategoryName(nameMap.get(c.getCategoryId())));
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
        List<Circle> list = circleMapper.selectList(wrapper);
        fillCategoryName(list);
        return list;
    }

    /**
     * 分页查询圈子正式成员
     */
    @Override
    public IPage<Map<String, Object>> listMembers(Long circleId, long current, long size, String keyword) {
        // 校验圈子存在
        getById(circleId);
        return circleMemberMapper.selectMembers(new Page<>(current, size), circleId, keyword);
    }

    /**
     * 圈主移出成员
     */
    @Override
    @Transactional
    public void removeMember(Long circleId, Long operatorId, Long userId) {
        // 操作者必须是该圈圈主
        Circle circle = assertOwner(circleId, operatorId);
        // 不能移除圈主（自己或任何圈主）
        if (userId.equals(circle.getOwnerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能移除圈主");
        }
        // 校验目标是否为该圈成员
        CircleUser target = circleUserMapper.selectOne(new LambdaQueryWrapper<CircleUser>()
                .eq(CircleUser::getCircleId, circleId)
                .eq(CircleUser::getUserId, userId));
        if (target == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该用户不在圈子中");
        }
        if (target.getRole() != null && target.getRole() == Constants.CIRCLE_MEMBER_OWNER) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能移除圈主");
        }
        // 复用退出逻辑：删除成员记录 + 成员数-1
        leave(circleId, userId);
    }

    /**
     * 圈主转让身份
     */
    @Override
    @Transactional
    public void transferOwner(Long circleId, Long operatorId, Long newOwnerId) {
        Circle circle = assertOwner(circleId, operatorId);
        if (newOwnerId.equals(operatorId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能转让给自己");
        }
        // 新圈主必须是该圈正式成员
        CircleUser newOwner = circleUserMapper.selectOne(new LambdaQueryWrapper<CircleUser>()
                .eq(CircleUser::getCircleId, circleId)
                .eq(CircleUser::getUserId, newOwnerId));
        if (newOwner == null || newOwner.getAuditStatus() == null
                || newOwner.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "新圈主必须是该圈正式成员");
        }
        // 新圈主 role 置 1
        newOwner.setRole(Constants.CIRCLE_MEMBER_OWNER);
        circleUserMapper.updateById(newOwner);
        // 原圈主 role 置 0
        CircleUser oldOwner = circleUserMapper.selectOne(new LambdaQueryWrapper<CircleUser>()
                .eq(CircleUser::getCircleId, circleId)
                .eq(CircleUser::getUserId, operatorId));
        if (oldOwner != null) {
            oldOwner.setRole(Constants.CIRCLE_MEMBER_NORMAL);
            circleUserMapper.updateById(oldOwner);
        }
        // 更新圈子 owner_id
        Circle update = new Circle();
        update.setId(circleId);
        update.setOwnerId(newOwnerId);
        circleMapper.updateById(update);
    }

    /**
     * 校验操作者是否为指定圈子的圈主，返回圈子实体
     *
     * @param circleId   圈子ID
     * @param operatorId 操作者用户ID
     * @return 圈子实体
     */
    private Circle assertOwner(Long circleId, Long operatorId) {
        Circle circle = getById(circleId);
        if (circle.getOwnerId() == null || !circle.getOwnerId().equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有圈主才能执行此操作");
        }
        return circle;
    }
}
