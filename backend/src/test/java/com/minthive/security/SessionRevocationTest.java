package com.minthive.security;

import com.minthive.common.BusinessException;
import com.minthive.controller.admin.AdminReportController;
import com.minthive.controller.admin.AdminUserController;
import com.minthive.entity.Post;
import com.minthive.entity.Comment;
import com.minthive.entity.Report;
import com.minthive.entity.User;
import com.minthive.mapper.*;
import com.minthive.service.SmsService;
import com.minthive.service.impl.UserServiceImpl;
import com.minthive.util.BcryptUtil;
import com.minthive.util.RedisUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionRevocationTest {
    @Mock UserMapper users;
    @Mock PostMapper posts;
    @Mock FollowMapper follows;
    @Mock CommentMapper comments;
    @Mock ReportMapper reports;
    @Mock SystemMsgMapper messages;
    @Mock RedisUtil redis;
    @Mock JwtUtils jwt;
    @Mock SmsService sms;
    @InjectMocks AdminUserController admin;
    @InjectMocks AdminReportController reportController;
    @InjectMocks UserServiceImpl service;

    @BeforeEach
    void login() { UserContext.set(new LoginUser(2L, "alice", 0)); }

    @AfterEach
    void clear() { UserContext.clear(); }

    @Test
    void removingRoleRevokesExistingSession() {
        admin.removeUserRole(2L);
        verify(redis).deleteLoginToken(2L);
    }

    @Test
    void changingAdminToOwnerRevokesExistingSession() {
        when(users.selectById(2L)).thenReturn(UserPrivacyTest.privateUser());
        admin.setUserRole(2L, Map.of("role", "1"));
        verify(redis).deleteLoginToken(2L);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4})
    void reportBansRevokeTheAccusedAuthorsSession(int targetType) {
        Report report = new Report();
        report.setId(7L);
        report.setTargetId(targetType == 4 ? 2L : 9L);
        report.setTargetType(targetType);
        report.setReporterId(3L);
        when(reports.selectById(7L)).thenReturn(report);
        if (targetType == 1) {
            Post post = new Post();
            post.setUserId(2L);
            when(posts.selectById(9L)).thenReturn(post);
        } else if (targetType == 2) {
            Comment comment = new Comment();
            comment.setUserId(2L);
            when(comments.selectById(9L)).thenReturn(comment);
        }
        reportController.punish(Map.of("workOrderId", 7L, "punishType", "ban"));
        verify(redis).deleteLoginToken(2L);
    }

    @Test
    void passwordChangeAndCancellationRevokeSessions() {
        User user = UserPrivacyTest.privateUser();
        user.setPassword(BcryptUtil.encode("old-password"));
        when(users.selectById(2L)).thenReturn(user);
        service.changePassword(2L, "old-password", "new-password");
        verify(redis).deleteLoginToken(2L);
        service.cancelAccount(2L);
        verify(redis, times(2)).deleteLoginToken(2L);
    }

    @Test
    void failedPasswordChangeDoesNotRevokeOrWrite() {
        User user = UserPrivacyTest.privateUser();
        user.setPassword(BcryptUtil.encode("old-password"));
        when(users.selectById(2L)).thenReturn(user);
        assertThrows(BusinessException.class, () -> service.changePassword(2L, "wrong", "new-password"));
        verify(users, never()).updateById(any(User.class));
        verifyNoInteractions(redis);
    }

    @Test
    void adminBanAndPasswordResetRevokeSessions() {
        admin.ban(Map.of("userId", 2L));
        admin.resetPassword(Map.of("userId", 2L, "newPassword", "new-password"));
        verify(redis, times(2)).deleteLoginToken(2L);
    }

    @Test
    void adminDetailRetainsManagementFieldsButNeverPasswordHashes() {
        when(users.selectById(2L)).thenReturn(UserPrivacyTest.privateUser());
        Map<String, Object> detail = admin.detail(2L).getData();
        assertEquals("alice", detail.get("account"));
        assertEquals("13800138000", detail.get("phone"));
        assertEquals("NORMAL", detail.get("status"));
        assertFalse(detail.containsKey("password"));
    }
}

