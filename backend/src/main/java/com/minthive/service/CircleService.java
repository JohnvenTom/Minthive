package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Circle;

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
}
