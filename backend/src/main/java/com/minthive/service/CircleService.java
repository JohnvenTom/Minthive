package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Circle;
import com.minthive.entity.Post;

import java.util.List;

/**
 * 圈子服务接口
 */
public interface CircleService {

    /**
     * 创建圈子
     *
     * @param circle 圈子实体
     * @return 圈子实体
     */
    Circle create(Circle circle);

    /**
     * 根据ID查询圈子
     *
     * @param id 圈子ID
     * @return 圈子实体
     */
    Circle getById(Long id);

    /**
     * 分页查询圈子
     *
     * @param current  当前页
     * @param size     每页大小
     * @param category 分类(可空)
     * @param keyword  关键词(可空)
     * @return 分页结果
     */
    Page<Circle> page(long current, long size, String category, String keyword);

    /**
     * 加入圈子
     *
     * @param circleId 圈子ID
     * @param userId   用户ID
     */
    void join(Long circleId, Long userId);

    /**
     * 退出圈子
     *
     * @param circleId 圈子ID
     * @param userId   用户ID
     */
    void leave(Long circleId, Long userId);

    /**
     * 更新圈子信息
     *
     * @param circle 圈子实体
     * @return 更新后的圈子
     */
    Circle update(Circle circle);

    /**
     * 查询圈内动态（分页）
     *
     * <p>功能描述：根据圈子ID查询该圈子内已审核通过的帖子列表，支持分页</p>
     *
     * @param id      圈子ID
     * @param current 当前页码，默认为1
     * @param size    每页大小，默认为10
     * @return 分页帖子结果
     */
    Page<Post> getCirclePosts(Long id, long current, long size);

    /**
     * 获取圈子分类列表
     *
     * <p>功能描述：返回系统预设的圈子分类名称列表</p>
     *
     * @return 分类名称列表
     */
    List<String> getCategories();

    /**
     * 获取推荐圈子列表
     *
     * <p>功能描述：返回成员数最多的前10个推荐圈子</p>
     *
     * @return 推荐圈子列表
     */
    List<Circle> recommendCircles();
}
