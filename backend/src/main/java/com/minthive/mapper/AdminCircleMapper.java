package com.minthive.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 管理后台-圈子管理 Mapper
 * <p>提供关联圈主查询，补全圈主昵称、帖子数等字段，对齐前端 CircleInfo / CircleApply 类型</p>
 */
@Mapper
public interface AdminCircleMapper {

    /**
     * 分页查询圈子列表（关联圈主）
     *
     * @param page     分页参数
     * @param keyword  搜索关键词
     * @param status   状态筛选
     * @param category 分类筛选
     * @return 分页结果
     */
    IPage<Map<String, Object>> selectCircleListWithOwner(
            Page<Map<String, Object>> page,
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("categoryId") Long categoryId);

    /**
     * 分页查询圈子创建申请列表（关联圈主昵称）
     *
     * @param page 分页参数
     * @return 创建申请列表
     */
    IPage<Map<String, Object>> selectCircleCreationApplyList(
            Page<Map<String, Object>> page);
}
