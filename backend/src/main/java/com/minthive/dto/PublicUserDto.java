package com.minthive.dto;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.minthive.entity.User;

/**
 * 公开用户投影。继承 User 仅为兼容现有 Page<User>/List<User> 服务契约；
 * 显式拷贝和序列化白名单确保实体新增字段不会自动进入公开响应。
 * 本人资料和管理后台继续使用各自的完整资料接口。
 */
@JsonIncludeProperties({"id", "nickname", "avatar", "bio", "gender",
        "postCount", "followCount", "fanCount", "isFollowing"})
public final class PublicUserDto extends User {

    public static PublicUserDto from(User user) {
        PublicUserDto profile = new PublicUserDto();
        profile.setId(user.getId());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setBio(user.getBio());
        profile.setGender(user.getGender());
        profile.setPostCount(user.getPostCount());
        profile.setFollowCount(user.getFollowCount());
        profile.setFanCount(user.getFanCount());
        profile.setIsFollowing(user.getIsFollowing());
        return profile;
    }
}
