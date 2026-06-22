package com.minthive.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Circle;
import com.minthive.entity.CircleCategory;
import com.minthive.entity.Post;

import java.util.List;
import java.util.Map;

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
     * @param current    当前页
     * @param size       每页大小
     * @param categoryId 分类ID(可空)
     * @param keyword    关键词(可空)
     * @return 分页结果
     */
    Page<Circle> page(long current, long size, Long categoryId, String keyword);

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
     * <p>功能描述：返回所有启用的圈子分类（从数据库查询）</p>
     *
     * @return 分类列表
     */
    List<CircleCategory> getCategories();

    /**
     * 创建自定义分类（已存在则返回原ID）
     *
     * @param name 分类名称
     * @return 分类ID
     */
    Long createCategory(String name);

    /**
     * 获取推荐圈子列表
     *
     * <p>功能描述：返回成员数最多的前10个推荐圈子</p>
     *
     * @return 推荐圈子列表
     */
    List<Circle> recommendCircles();

    /**
     * 分页查询圈子正式成员（关联用户昵称/头像）
     *
     * <p>仅返回 audit_status=1 的正式成员；圈主置顶，其余按入圈时间倒序</p>
     *
     * @param circleId 圈子ID
     * @param current  当前页码
     * @param size     每页大小
     * @param keyword  昵称关键词（可空）
     * @return 分页成员列表（每条含 userId, nickname, avatar, role, joinTime）
     */
    IPage<Map<String, Object>> listMembers(Long circleId, long current, long size, String keyword);

    /**
     * 圈主移出成员
     *
     * <p>校验操作者必须是该圈圈主；目标不能是圈主；随后复用退出逻辑删除记录并扣减成员数</p>
     *
     * @param circleId   圈子ID
     * @param operatorId 操作者（圈主）用户ID
     * @param userId     被移出的用户ID
     */
    void removeMember(Long circleId, Long operatorId, Long userId);

    /**
     * 圈主转让身份
     *
     * <p>校验操作者必须是该圈圈主；新圈主必须是该圈正式成员。
     * 事务内：新圈主 role 置 1，原圈主 role 置 0，更新 circle.owner_id</p>
     *
     * @param circleId    圈子ID
     * @param operatorId  操作者（原圈主）用户ID
     * @param newOwnerId  新圈主用户ID
     */
    void transferOwner(Long circleId, Long operatorId, Long newOwnerId);
}
