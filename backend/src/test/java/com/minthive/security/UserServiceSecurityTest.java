package com.minthive.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.common.BusinessException;
import com.minthive.entity.Post;
import com.minthive.entity.User;
import com.minthive.mapper.FollowMapper;
import com.minthive.mapper.LikeCollectMapper;
import com.minthive.mapper.PostMapper;
import com.minthive.mapper.UserMapper;
import com.minthive.service.ContentAccessService;
import com.minthive.service.PostService;
import com.minthive.service.SmsService;
import com.minthive.service.impl.UserServiceImpl;
import com.minthive.util.BcryptUtil;
import com.minthive.util.RedisUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceSecurityTest {
    @Mock UserMapper users;
    @Mock PostMapper posts;
    @Mock FollowMapper follows;
    @Mock LikeCollectMapper likes;
    @Mock PostService postService;
    @Mock ContentAccessService access;
    @Mock JwtUtils jwt;
    @Mock RedisUtil redis;
    @Mock SmsService sms;
    @InjectMocks UserServiceImpl service;

    @AfterEach
    void clear() { UserContext.clear(); }

    @Test
    void userSearchIsPublicAndCannotRevealPrivateProfileFields() {
        Page<User> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(UserPrivacyTest.privateUser()));
        when(users.selectPage(any(), any())).thenReturn(page);
        UserPrivacyTest.assertPublic(new ObjectMapper().findAndRegisterModules()
                .valueToTree(service.searchByKeyword("alice", 1, 10).getRecords().get(0)));
    }

    @Test
    void userPostsApplySharedVisibilityForAnonymousAndAuthenticatedViewersAndEnrichCounts() {
        LambdaQueryWrapper<Post> visible = new LambdaQueryWrapper<>();
        when(access.visiblePosts(null)).thenReturn(visible);
        when(access.visiblePosts(3L)).thenReturn(visible);
        Page<Post> page = new Page<>(1, 10);
        when(posts.selectPage(any(), any())).thenReturn(page);
        assertSame(page, service.getUserPosts(2L, 1, 10));
        UserContext.set(new LoginUser(3L, "viewer", 0));
        assertSame(page, service.getUserPosts(2L, 1, 10));
        verify(access).visiblePosts(null);
        verify(access).visiblePosts(3L);
        verify(posts, times(2)).selectPage(any(), same(visible));
        verify(postService, times(2)).enrichPageCounts(page);
    }

    @Test
    void savedLikesAndCollectionsCannotExposePostsThatAreNoLongerReadable() {
        UserContext.set(new LoginUser(2L, "alice", 0));
        var relation = new com.minthive.entity.LikeCollect();
        relation.setTargetId(9L);
        Page<com.minthive.entity.LikeCollect> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(relation));
        when(likes.selectPage(any(), any())).thenReturn(page);
        when(access.visiblePosts(2L)).thenReturn(new LambdaQueryWrapper<>());
        when(posts.selectList(any())).thenReturn(List.of());
        assertTrue(service.getCollects(2L, 1, 10).getRecords().isEmpty());
        assertTrue(service.getLikes(2L, 1, 10).getRecords().isEmpty());
        verify(access, times(2)).visiblePosts(2L);
    }

    @ParameterizedTest
    @CsvSource({"0,0,0", "1,1,0", "1,2,0", "1,0,1"})
    void passwordLoginCannotRecreateSessionsForInactiveUsers(int status, int cancellation, int deleted) {
        User user = UserPrivacyTest.privateUser();
        user.setPassword(BcryptUtil.encode("secret"));
        user.setStatus(status);
        user.setCancelStatus(cancellation);
        user.setDeleted(deleted);
        when(users.selectOne(any())).thenReturn(user);
        assertThrows(BusinessException.class, () -> service.login("alice", "secret"));
        verify(redis, never()).cacheLoginToken(any(), any());
    }

    @Test
    void smsLoginCannotRecreatePendingCancellationSession() {
        User user = UserPrivacyTest.privateUser();
        user.setCancelStatus(1);
        when(users.selectOne(any())).thenReturn(user);
        when(sms.verifyCode("13800138000", "123456")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.loginBySms("13800138000", "123456"));
        verify(redis, never()).cacheLoginToken(any(), any());
    }
}
