package com.minthive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Post;

import java.util.List;

/**
 * 帖子服务接口
 */
public interface PostService {

    /**
     * 发布帖子
     *
     * @param post 帖子实体
     * @return 发布后的帖子
     */
    Post publish(Post post);

    /**
     * 根据ID查询帖子
     *
     * @param id 帖子ID
     * @return 帖子实体
     */
    Post getById(Long id);

    /**
     * 分页查询帖子
     *
     * @param current  当前页
     * @param size     每页大小
     * @param circleId 圈子ID(可空)
     * @return 分页结果
     */
    Page<Post> page(long current, long size, Long circleId);

    /**
     * 删除帖子
     *
     * @param id     帖子ID
     * @param userId 操作用户ID
     */
    void delete(Long id, Long userId);

    /**
     * 编辑帖子（修改内容、图片、可见性）
     *
     * <p>功能描述：更新帖子的文案内容、图片地址或可见范围，
     * 仅允许帖子作者本人操作</p>
     *
     * @param id     帖子ID
     * @param post   包含更新字段的帖子实体（content/images/visibility）
     * @param userId 操作用户ID（用于权限校验）
     * @return 更新后的帖子
     * @throws BusinessException 无权操作或帖子不存在时抛出
     */
    Post update(Long id, Post post, Long userId);

    /**
     * 切换帖子可见性（隐藏/公开）
     *
     * <p>功能描述：在"仅自己可见"(2)和原可见性之间切换。
     * 隐藏时设为 visibility=2，公开时恢复为 visibility=0(公开)</p>
     *
     * @param id        帖子ID
     * @param visibility 目标可见性: 0=公开, 1=仅粉丝, 2=仅自己(隐藏)
     * @param userId    操作用户ID
     * @return 更新后的帖子
     */
    Post toggleVisibility(Long id, Integer visibility, Long userId);

    /**
     * 审核帖子
     *
     * @param id     帖子ID
     * @param status 审核状态
     */
    void audit(Long id, Integer status);

    /**
     * 按关键词搜索帖子（内容模糊匹配）
     *
     * @param keyword 搜索关键词(可空)
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    Page<Post> searchByKeyword(String keyword, long current, long size);

    /**
     * 获取首页信息流（推荐/最新/最热）
     *
     * @param sortType 排序类型: latest(最新) / hot(最热)
     * @param current  当前页
     * @param size     每页大小
     * @return 分页帖子列表
     */
    Page<Post> feed(String sortType, long current, long size);

    /**
     * 填充分页结果中每条帖子的统计数据
     *
     * <p>功能描述：遍历分页中的帖子，实时查询并填充 commentCount/likeCount/collectCount/shareCount，
     * 以及发布者昵称、头像和当前用户的交互状态</p>
     *
     * @param page 分页帖子列表
     */
    void enrichPageCounts(Page<Post> page);

    /**
     * 转发帖子
     *
     * <p>功能描述：创建一条新帖，内容引用原帖，sharePostId 指向原帖ID</p>
     *
     * @param post       转发时的附加内容（可为空）
     * @param sharePostId 原帖ID
     * @param userId     转发用户ID
     * @return 创建的转发帖子
     */
    Post share(Post post, Long sharePostId, Long userId);

    /**
     * 保存草稿
     *
     * <p>功能描述：将帖子以草稿形式保存（auditStatus=0），不进行敏感词审核</p>
     *
     * @param post 帖子实体（内容、图片等）
     * @param userId 发布用户ID
     * @return 保存后的草稿帖子
     */
    Post saveDraft(Post post, Long userId);

    /**
     * 查询当前用户的草稿列表
     *
     * <p>功能描述：查询指定用户所有 auditStatus=0 的草稿帖子，按更新时间倒序排列</p>
     *
     * @param userId 用户ID
     * @return 草稿帖子列表（按更新时间倒序）
     */
    List<Post> getDraftList(Long userId);

    /**
     * 获取帖子转发链（被转发列表）
     *
     * <p>功能描述：查询所有 sharePostId 指向指定帖子的转发帖，
     * 返回完整的转发者信息（昵称、头像）和转发内容，支持分页</p>
     *
     * @param postId  原帖ID
     * @param current 当前页码
     * @param size    每页大小
     * @return 分页的转发帖子列表（已填充用户昵称、头像等关联数据）
     */
    Page<Post> getShareChain(Long postId, long current, long size);
}
