package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 动态帖子 Mapper 接口
 *
 * <p>功能描述：提供 post 表的基础 CRUD 操作及自定义排序查询</p>
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 按热度（点赞数）分页查询公开帖子
     *
     * <p>通过子查询从 like_collect 表实时统计每条帖子的点赞数并降序排列，
     * 点赞数相同时按发布时间倒序，确保热门帖子优先展示</p>
     *
     * @param page  MyBatis-Plus 分页参数（current, size）
     * @return 按热度排序的帖子分页结果
     */
    Page<Post> selectHotFeed(Page<Post> page);

    /**
     * 按综合推荐算法分页查询公开帖子
     *
     * <p>综合评分 = 点赞数×1 + 评论数×2 + 收藏数×3，权重体现评论和收藏的更高参与度，
     * 同分时按发布时间倒序，兼顾内容质量和新鲜度</p>
     *
     * @param page  MyBatis-Plus 分页参数（current, size）
     * @return 按综合评分排序的帖子分页结果
     */
    Page<Post> selectRecommendFeed(Page<Post> page);
}
