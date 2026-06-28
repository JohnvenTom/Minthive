package com.minthive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 动态帖子 Mapper 接口
 *
 * <p>功能描述：提供 post 表的基础 CRUD 操作及自定义排序查询</p>
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 按热度（点赞数）分页查询公开帖子
     */
    Page<Post> selectHotFeed(Page<Post> page);

    /**
     * 按综合推荐算法分页查询公开帖子
     */
    Page<Post> selectRecommendFeed(Page<Post> page);

    /**
     * 查询候选帖子池（已审核、公开、未删除），附带热度分数，按时间倒序
     */
    List<Post> selectCandidatePool(@Param("limit") int limit);

    /**
     * 按ID列表查询帖子并保持顺序
     */
    List<Post> selectByIdsOrdered(@Param("ids") List<Long> ids);
}
