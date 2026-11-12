package com.minthive.ai;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minthive.common.BusinessException;
import com.minthive.common.ResultCode;
import com.minthive.entity.*;
import com.minthive.mapper.*;
import com.minthive.service.ContentAccessService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiToolSecurityTest {
    @Mock PostMapper posts;
    @Mock UserMapper users;
    @Mock CircleMapper circles;
    @Mock CircleUserMapper circleUsers;
    @Mock CommentMapper comments;
    @Mock LikeCollectMapper likes;
    @Mock FollowMapper follows;
    @Mock SystemMsgMapper notifications;
    @Mock AnnouncementMapper announcements;
    @Mock CircleCategoryMapper categories;
    @Mock MessageMapper messages;
    @Mock ContentAccessService access;
    @InjectMocks AiToolService tools;

    @BeforeEach
    void initializeMapperMetadata() {
        for (Class<?> entity : List.of(Post.class, Message.class, SystemMsg.class)) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), "test"), entity);
        }
    }

    @Test
    void privatePostDetailDoesNotExposeContentOrNavigation() {
        Post secret = new Post();
        secret.setId(7L);
        secret.setUserId(99L);
        secret.setVisibility(2);
        secret.setAuditStatus(1);
        secret.setContent("PRIVATE_SECRET");
        when(posts.selectById(7L)).thenReturn(secret);
        doThrow(new BusinessException(ResultCode.POST_NOT_EXISTS))
                .when(access).requireReadable(secret, 1L);

        ToolResult result = tools.execute(new IntentParser.ParsedIntent(
                IntentParser.IntentType.GET_POST, Map.of("id", 7L)), 1L);

        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertNull(result.getNavigation());
        assertFalse(result.getDataSummary().contains("PRIVATE_SECRET"));
        verifyNoInteractions(users, likes, comments);
    }

    @ParameterizedTest
    @EnumSource(value = IntentParser.IntentType.class, names = {
            "SEARCH_POSTS", "GET_TRENDING", "GET_LATEST_POSTS", "GET_RECOMMEND",
            "SEARCH_POSTS_BY_TAGS", "GET_CIRCLE_POSTS", "GET_USER_POSTS", "GET_COLLECTIONS"
    })
    void postListsUseVisibilityBeforePagination(IntentParser.IntentType type) {
        var visible = new LambdaQueryWrapper<Post>();
        when(access.visiblePosts(1L)).thenReturn(visible);
        Post readable = new Post();
        readable.setId(10L);
        readable.setContent("VISIBLE");
        when(posts.selectPage(any(), any())).thenAnswer(invocation -> {
            assertSame(visible, invocation.getArgument(1), "Visibility must be applied inside SQL pagination");
            Page<Post> page = invocation.getArgument(0);
            return page.setRecords(List.of(readable)).setTotal(1);
        });
        Circle circle = new Circle();
        circle.setName("circle");
        User user = new User();
        user.setNickname("user");
        lenient().when(circles.selectById(3L)).thenReturn(circle);
        lenient().when(users.selectById(99L)).thenReturn(user);
        ToolResult result = tools.execute(new IntentParser.ParsedIntent(type,
                Map.of("keyword", "key", "circleId", 3L, "userId", 99L)), 1L);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().get("count"));
        assertFalse(result.getDataSummary().contains("PRIVATE_SECRET"));
        verify(access).visiblePosts(1L);
        verify(posts, never()).selectHotFeed(any());
        verify(posts, never()).selectRecommendFeed(any());
        verify(posts, never()).selectById(any(java.io.Serializable.class));
        verifyNoInteractions(likes);
    }

    @ParameterizedTest
    @EnumSource(value = IntentParser.IntentType.class,
            names = {"GET_COMMENTS", "GET_POST_LIKES"})
    void privatePostChildrenAreNotRead(IntentParser.IntentType type) {
        Post secret = new Post();
        secret.setContent("PRIVATE_SECRET");
        when(posts.selectById(7L)).thenReturn(secret);
        doThrow(new BusinessException(ResultCode.POST_NOT_EXISTS))
                .when(access).requireReadable(secret, 1L);
        ToolResult result = tools.execute(new IntentParser.ParsedIntent(type, Map.of("postId", 7L)), 1L);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertNull(result.getNavigation());
        verifyNoInteractions(comments, likes, users);
    }

    @ParameterizedTest
    @EnumSource(value = IntentParser.IntentType.class, names = {
            "GET_MESSAGES", "GET_MESSAGE_CENTER", "GET_NOTIFICATIONS", "GET_COLLECTIONS"
    })
    void forgedSensitiveUserIdsAreReplacedByTrustedViewer(IntentParser.IntentType type) {
        if (type == IntentParser.IntentType.GET_MESSAGES || type == IntentParser.IntentType.GET_MESSAGE_CENTER) {
            when(messages.selectList(any())).thenReturn(List.of());
        }
        if (type == IntentParser.IntentType.GET_NOTIFICATIONS || type == IntentParser.IntentType.GET_MESSAGE_CENTER) {
            when(notifications.selectPage(any(), any())).thenReturn(new Page<>());
        }
        if (type == IntentParser.IntentType.GET_MESSAGE_CENTER) {
            when(announcements.selectList(any())).thenReturn(List.of());
        }
        if (type == IntentParser.IntentType.GET_COLLECTIONS) {
            when(access.visiblePosts(1L)).thenReturn(new LambdaQueryWrapper<>());
            when(posts.selectPage(any(), any())).thenReturn(new Page<>());
        }
        var params = Map.<String, Object>of("userId", 99L);
        tools.execute(new IntentParser.ParsedIntent(type, params), 1L);
        assertEquals(99L, params.get("userId"), "Do not mutate the caller's model output");
        int queries = 0;
        for (Object mapper : List.of(messages, notifications, posts)) {
            for (var invocation : mockingDetails(mapper).getInvocations()) {
                for (Object argument : invocation.getArguments()) {
                    if (argument instanceof AbstractWrapper<?, ?, ?> query) {
                        query.getSqlSegment();
                        assertFalse(query.getParamNameValuePairs().containsValue(99L));
                        assertTrue(query.getParamNameValuePairs().containsValue(1L));
                        queries++;
                    }
                }
            }
        }
        assertTrue(queries > 0);
    }

    @ParameterizedTest
    @EnumSource(value = IntentParser.IntentType.class, names = {
            "GET_MESSAGES", "GET_MESSAGE_CENTER", "GET_NOTIFICATIONS", "GET_COLLECTIONS"
    })
    void anonymousViewerCannotUseModelIdentity(IntentParser.IntentType type) {
        ToolResult result = tools.execute(new IntentParser.ParsedIntent(type, Map.of("userId", 99L)), null);
        assertFalse(result.isSuccess());
        verifyNoInteractions(messages, notifications, likes, posts, access);
    }

    @ParameterizedTest
    @EnumSource(value = IntentParser.IntentType.class, names = {"GET_POST", "GET_COMMENTS", "GET_POST_LIKES"})
    void missingAndUnreadablePostsHaveIndistinguishableResponses(IntentParser.IntentType type) {
        Post secret = new Post();
        when(posts.selectById(7L)).thenReturn(secret);
        doThrow(new BusinessException(ResultCode.POST_NOT_EXISTS)).when(access).requireReadable(secret, 1L);
        doThrow(new BusinessException(ResultCode.POST_NOT_EXISTS)).when(access).requireReadable(null, 1L);
        ToolResult denied = tools.execute(new IntentParser.ParsedIntent(type, Map.of("id", 7L, "postId", 7L)), 1L);
        ToolResult absent = tools.execute(new IntentParser.ParsedIntent(type, Map.of("id", 8L, "postId", 8L)), 1L);
        assertEquals(denied, absent);
        assertFalse(absent.isSuccess());
        verifyNoInteractions(comments, likes, users);
    }
}
