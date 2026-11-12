package com.minthive.service;

import com.minthive.entity.Circle;
import com.minthive.entity.Follow;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.mapper.CircleMapper;
import com.minthive.mapper.CircleUserMapper;
import com.minthive.mapper.CommentMapper;
import com.minthive.mapper.FollowMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentAccessServiceTest {
    private final PostMapper posts = mock(PostMapper.class);
    private final CircleMapper circles = mock(CircleMapper.class);
    private final CircleUserMapper members = mock(CircleUserMapper.class);
    private final CommentMapper comments = mock(CommentMapper.class);
    private final FollowMapper follows = mock(FollowMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final ContentAccessService access = new ContentAccessService(posts, circles, members, comments, follows, users);

    private Post post(int visibility, Integer audit, Long author, Long circleId) {
        Post post = new Post();
        post.setId(100L);
        post.setUserId(author);
        post.setVisibility(visibility);
        post.setAuditStatus(audit);
        post.setCircleId(circleId);
        post.setDeleted(0);
        return post;
    }

    @Test
    void anonymousCannotReadFollowerOnlyPost() {
        assertFalse(access.canRead(post(1, 1, 3L, null), null));
    }

    @Test
    void followerCanReadFollowerOnlyPost() {
        when(follows.selectCount(any())).thenReturn(1L);
        assertTrue(access.canRead(post(1, 1, 3L, null), 5L));
    }

    @Test
    void nonFollowerCannotReadFollowerOnlyPost() {
        when(follows.selectCount(any())).thenReturn(0L);
        assertFalse(access.canRead(post(1, 1, 3L, null), 6L));
    }

    @Test
    void ownerCanReadOwnUnreviewedPost() {
        assertTrue(access.canRead(post(2, 0, 7L, null), 7L));
    }

    @Test
    void nonOwnerCannotReadUnreviewedPost() {
        assertFalse(access.canRead(post(0, 0, 7L, null), 8L));
    }

    @Test
    void privateCirclePostRequiresFormalMembership() {
        Circle circle = new Circle();
        circle.setId(1L);
        circle.setType(1);
        circle.setStatus(1);
        when(circles.selectById(1L)).thenReturn(circle);
        when(members.selectCount(any())).thenReturn(0L);
        assertFalse(access.canRead(post(0, 1, 3L, 1L), 6L));
        when(members.selectCount(any())).thenReturn(1L);
        assertTrue(access.canRead(post(0, 1, 3L, 1L), 6L));
    }

    @Test
    void shareCannotWidenPrivateOriginal() {
        Post original = post(2, 1, 3L, null);
        original.setId(99L);
        when(posts.selectById(99L)).thenReturn(original);
        Post shared = post(0, 1, 8L, null);
        shared.setSharePostId(99L);
        assertFalse(access.canRead(shared, null));
    }

    @Test
    void adminCanReadForOperations() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(2);
        admin.setStatus(1);
        admin.setCancelStatus(0);
        when(users.selectById(1L)).thenReturn(admin);
        assertTrue(access.canRead(post(2, 0, 7L, null), 1L));
    }
}
