package com.minthive;

import com.minthive.ai.AiContext;
import com.minthive.ai.AiService;
import com.minthive.ai.ImageModerationService;
import com.minthive.entity.Post;
import com.minthive.mapper.PostMapper;
import com.minthive.security.UserContext;
import com.minthive.service.ActionRateLimiter;
import com.minthive.service.ContentAccessService;
import com.minthive.service.impl.PostServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostSecurityTest {
    private final PostMapper posts = mock(PostMapper.class);
    private final AiContext ai = mock(AiContext.class);
    private final ContentAccessService access = mock(ContentAccessService.class);
    private final ActionRateLimiter limiter = mock(ActionRateLimiter.class);
    private final ImageModerationService imageModeration = mock(ImageModerationService.class);

    private PostServiceImpl service() {
        PostServiceImpl service = mock(PostServiceImpl.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "postMapper", posts);
        ReflectionTestUtils.setField(service, "aiContext", ai);
        ReflectionTestUtils.setField(service, "contentAccessService", access);
        ReflectionTestUtils.setField(service, "actionRateLimiter", limiter);
        ReflectionTestUtils.setField(service, "imageModerationService", imageModeration);
        return service;
    }

    private Post textPost() {
        Post post = new Post();
        post.setUserId(1L);
        post.setContent("test content");
        post.setVisibility(0);
        return post;
    }

    @AfterEach
    void clearIdentity() {
        UserContext.clear();
    }

    @Test
    void unavailableModerationMustNotPublish() {
        when(ai.detectContent(anyString(), isNull())).thenThrow(new IllegalStateException("offline"));
        Post post = textPost();
        service().publish(post);
        assertEquals(0, post.getAuditStatus());
        verify(posts).insert(post);
    }

    @Test
    void moderationIncompleteMustNotPublish() {
        AiService.AiDetectResult result = new AiService.AiDetectResult(false, "无法检测", "unknown");
        result.available = false;
        when(ai.detectContent(anyString(), isNull())).thenReturn(result);
        Post post = textPost();
        service().publish(post);
        assertEquals(0, post.getAuditStatus());
    }

    @Test
    void cleanTextMayPassAfterRealModeration() {
        when(ai.detectContent(anyString(), isNull())).thenReturn(new AiService.AiDetectResult(false, "无", "low"));
        Post post = textPost();
        service().publish(post);
        assertEquals(1, post.getAuditStatus());
    }

    @Test
    void mediaNeverPassesWithoutImageModeration() {
        when(imageModeration.moderate(anyString(), anyString()))
                .thenReturn(AiService.AiDetectResult.unavailable("图片审核不可用"));
        Post post = textPost();
        post.setImages("[\"/api/file/content/example\"]");
        service().publish(post);
        assertEquals(0, post.getAuditStatus());
        verify(imageModeration).moderate(anyString(), anyString());
        verify(ai, never()).detectContent(anyString(), any());
    }

    @Test
    void updateInvalidVisibilityRejected() {
        Post existing = textPost();
        existing.setId(5L);
        PostServiceImpl service = service();
        doReturn(existing).when(service).getById(5L, 1L);
        Post patch = new Post();
        patch.setVisibility(9);
        assertThrows(RuntimeException.class, () -> service.update(5L, patch, 1L));
        verify(posts, never()).updateById(any(Post.class));
    }

    @Test
    void shareOfNonPublicOriginalRejected() {
        Post original = textPost();
        original.setId(9L);
        when(posts.selectById(9L)).thenReturn(original);
        when(access.isPubliclyShareable(original)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service().share(new Post(), 9L, 2L));
        verify(posts, never()).insert(any(Post.class));
    }
}
