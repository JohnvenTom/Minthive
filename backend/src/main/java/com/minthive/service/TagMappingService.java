package com.minthive.service;

import com.minthive.common.TagMappingConstants;
import com.minthive.entity.Circle;
import com.minthive.entity.CircleCategory;
import com.minthive.entity.Post;
import com.minthive.mapper.CircleCategoryMapper;
import com.minthive.mapper.CircleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagMappingService {

    private final CircleMapper circleMapper;
    private final CircleCategoryMapper circleCategoryMapper;

    /**
     * 解析帖子关联的所有兴趣标签
     * 来源1: 帖子 tags 字段匹配
     * 来源2: 帖子所属圈子的分类名匹配
     *
     * @param post 帖子
     * @return 匹配的兴趣标签 key 集合
     */
    public Set<String> resolvePostTags(Post post) {
        if (post == null) return Collections.emptySet();
        Set<String> tags = TagMappingConstants.matchTagsFromString(post.getTags());
        // 补充圈子分类标签
        if (post.getCircleId() != null) {
            Circle circle = circleMapper.selectById(post.getCircleId());
            if (circle != null && circle.getCategoryId() != null) {
                CircleCategory category = circleCategoryMapper.selectById(circle.getCategoryId());
                if (category != null && category.getName() != null) {
                    tags.addAll(TagMappingConstants.matchTags(List.of(category.getName())));
                }
            }
        }
        return tags;
    }

    /**
     * 解析圈子关联的兴趣标签（通过分类名）
     */
    public Set<String> resolveCircleTags(Circle circle) {
        if (circle == null || circle.getCategoryId() == null) return Collections.emptySet();
        CircleCategory category = circleCategoryMapper.selectById(circle.getCategoryId());
        if (category == null || category.getName() == null) return Collections.emptySet();
        return TagMappingConstants.matchTags(List.of(category.getName()));
    }
}
