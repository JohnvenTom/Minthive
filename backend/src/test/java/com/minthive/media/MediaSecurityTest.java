package com.minthive.media;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.common.BusinessException;
import com.minthive.common.Result;
import com.minthive.common.ResultCode;
import com.minthive.config.JwtConfig;
import com.minthive.config.MinioConfig;
import com.minthive.controller.FileController;
import com.minthive.entity.*;
import com.minthive.mapper.*;
import com.minthive.security.LoginUser;
import com.minthive.security.UserContext;
import com.minthive.service.ContentAccessService;
import com.minthive.service.impl.FileServiceImpl;
import com.minthive.util.MinioUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MediaSecurityTest {
    private final ObjectMapper json = new ObjectMapper();
    private final MinioUtil storage = mock(MinioUtil.class);
    private final ContentAccessService content = mock(ContentAccessService.class);
    private final PostMapper posts = mock(PostMapper.class);
    private final CommentMapper comments = mock(CommentMapper.class);
    private final MessageMapper messages = mock(MessageMapper.class);
    private final UserMapper users = mock(UserMapper.class);
    private final CircleMapper circles = mock(CircleMapper.class);
    private final BannerMapper banners = mock(BannerMapper.class);
    private MediaLocator locator;
    private MediaAccessService access;
    private MediaLocator.Ref ref;
    private MediaTicketService tickets;
    private FileServiceImpl files;
    private MediaBodyAdvice advice;
    private final MinioUtil.Metadata metadata = new MinioUtil.Metadata(10, "image/png", 1L);

    @BeforeEach
    void setUp() throws Exception {
        MinioConfig config = new MinioConfig();
        config.setEndpoint("http://storage.invalid:9000");
        config.setPublicUrl("https://images.invalid");
        config.setBucket("media");
        locator = new MediaLocator(config);
        ref = locator.fromName("users/1/0123456789abcdef0123456789abcdef.png");
        access = new MediaAccessService(storage, locator, content, posts, comments, messages, users, circles, banners, json);
        files = new FileServiceImpl(storage, locator, access);
        JwtConfig jwt = new JwtConfig();
        jwt.setSecret("test-only-root-key-at-least-32-bytes-long");
        tickets = new MediaTicketService(jwt, Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC));
        advice = new MediaBodyAdvice(json, locator, access, tickets);
        when(storage.stat(ref)).thenReturn(metadata);
        when(users.selectById(anyLong())).thenAnswer(inv -> user(inv.getArgument(0)));
        UserContext.set(new LoginUser(1L, "owner", 0));
    }

    @AfterEach
    void clear() { UserContext.clear(); }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setStatus(1);
        user.setCancelStatus(0);
        user.setDeleted(0);
        return user;
    }

    private Post post(Long author, String url) {
        Post post = new Post();
        post.setUserId(author);
        post.setId(author + 100);
        post.setImages("[\"" + url + "\"]");
        return post;
    }

    private Message message(String url) {
        Message message = new Message();
        message.setFromUserId(1L);
        message.setToUserId(2L);
        message.setMsgType(1);
        message.setContent(url);
        return message;
    }

    @Test
    void otherUserCannotDeleteEvenWithOwnersTicket() throws Exception {
        UserContext.set(new LoginUser(2L, "other", 0));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> files.delete(ref.canonical() + "?ticket=" + tickets.issue(ref, 1L)));
        assertEquals(403, ex.getCode());
        verify(storage, never()).delete(any());
    }

    @Test
    void ownerAndCurrentAdminCanDeleteButFailuresPropagate() throws Exception {
        files.delete(ref.canonical());
        UserContext.set(new LoginUser(2L, "admin", 2));
        when(content.isAdmin(2L)).thenReturn(true);
        files.delete(ref.canonical());
        verify(storage, times(2)).delete(ref);
        doThrow(new IOException("offline")).when(storage).delete(ref);
        assertEquals(500, assertThrows(BusinessException.class, () -> files.delete(ref.canonical())).getCode());
    }

    @Test
    void unknownLegacyOwnerCannotBeDeletedEvenByAdmin() throws Exception {
        UserContext.set(new LoginUser(2L, "admin", 2));
        when(content.isAdmin(2L)).thenReturn(true);
        String legacy = "https://images.invalid/media/2025/01/01/0123456789abcdef0123456789abcdef.png";
        assertEquals(403, assertThrows(BusinessException.class, () -> files.delete(legacy)).getCode());
        verify(storage, never()).delete(any());
    }

    @Test
    void roleClaimDoesNotAuthorizeDeletionAndExternalUrlsAreRejected() throws Exception {
        UserContext.set(new LoginUser(2L, "forged-admin", 2));
        assertThrows(BusinessException.class, () -> files.delete(ref.canonical()));
        assertThrows(BusinessException.class, () -> files.delete("https://external.invalid/image.png"));
        verify(storage, never()).delete(any());
    }

    @Test
    void privatePostAndPublicCopyDoNotBecomePublic() {
        Post privatePost = post(1L, ref.canonical());
        Post copiedPost = post(2L, ref.canonical());
        when(posts.selectList(any())).thenReturn(List.of(copiedPost, privatePost));
        when(content.canRead(copiedPost, null)).thenReturn(true);
        assertFalse(access.canRead(ref, metadata, null));
        when(content.canRead(privatePost, 2L)).thenReturn(true);
        assertTrue(access.canRead(ref, metadata, 2L));
        assertThrows(BusinessException.class, () -> access.requireReference(ref, 2L));
        assertDoesNotThrow(() -> access.requireReference(ref, 1L));
    }

    @Test
    void publicCopyAloneCannotDelegateAnotherOwnersUnattachedObject() {
        Post copy = post(2L, ref.canonical());
        User avatarCopy = user(2L);
        avatarCopy.setAvatar(ref.canonical());
        when(posts.selectList(any())).thenReturn(List.of(copy));
        when(users.selectList(any())).thenReturn(List.of(avatarCopy));
        when(content.canRead(copy, null)).thenReturn(true);
        assertFalse(access.canRead(ref, metadata, null));
    }

    @Test
    void chatMediaCannotGainPublicAccessViaAvatarBannerOrPublicPost() {
        Post publicPost = post(1L, ref.canonical());
        when(content.canRead(publicPost, null)).thenReturn(true);
        when(content.canRead(publicPost, 2L)).thenReturn(true);
        when(posts.selectList(any())).thenReturn(List.of(publicPost));
        when(messages.selectList(any())).thenReturn(List.of(message(ref.canonical())));
        User avatar = user(1L);
        avatar.setAvatar(ref.canonical());
        when(users.selectList(any())).thenReturn(List.of(avatar));
        assertFalse(access.canRead(ref, metadata, null));
        assertFalse(access.canRead(ref, metadata, 3L));
        assertTrue(access.canRead(ref, metadata, 2L));
        assertThrows(BusinessException.class, () -> access.requireReference(ref, 2L));
    }

    @Test
    void legacyConflictingReferencesFailClosed() throws Exception {
        MediaLocator.Ref old = locator.fromName("2025/01/01/0123456789abcdef0123456789abcdef.png");
        String url = locator.aliases(old).get(1);
        Post publicCopy = post(2L, old.canonical());
        when(posts.selectList(any())).thenReturn(List.of(publicCopy));
        when(content.canRead(publicCopy, null)).thenReturn(true);
        when(messages.selectList(any())).thenReturn(List.of(message(url)));
        MinioUtil.Metadata legacyMeta = new MinioUtil.Metadata(1, "image/png", null);
        when(storage.stat(old)).thenReturn(legacyMeta);
        assertFalse(access.canRead(old, legacyMeta, null));
        assertThrows(BusinessException.class, () -> access.requireReference(old, 1L));
    }

    @Test
    void publicAvatarAndApprovedPostMayIssueAnonymousTickets() {
        User owner = user(1L);
        owner.setAvatar(ref.canonical());
        when(users.selectList(any())).thenReturn(List.of(owner));
        assertDoesNotThrow(() -> access.requireReadable(ref, null));
        when(users.selectList(any())).thenReturn(List.of());
        Post post = post(1L, ref.canonical());
        when(posts.selectList(any())).thenReturn(List.of(post));
        when(content.canRead(post, null)).thenReturn(true);
        assertTrue(access.canRead(ref, metadata, null));
        when(content.canRead(post, null)).thenReturn(false);
        assertFalse(access.canRead(ref, metadata, null));
    }

    @Test
    void commentVisibilityComesFromItsPostAndCircleCoverFromCircle() {
        Comment comment = new Comment();
        comment.setUserId(1L);
        comment.setPostId(10L);
        comment.setImages("[\"" + ref.canonical() + "\"]");
        Post parent = post(3L, "");
        when(posts.selectById(10L)).thenReturn(parent);
        when(comments.selectList(any())).thenReturn(List.of(comment));
        assertFalse(access.canRead(ref, metadata, 2L));
        when(content.canRead(parent, 2L)).thenReturn(true);
        assertTrue(access.canRead(ref, metadata, 2L));
        when(comments.selectList(any())).thenReturn(List.of());
        Circle circle = new Circle();
        circle.setId(10L);
        circle.setOwnerId(1L);
        circle.setBanner(ref.canonical());
        when(circles.selectList(any())).thenReturn(List.of(circle));
        when(content.canReadCircle(10L, null)).thenReturn(true);
        assertTrue(access.canRead(ref, metadata, null));
        when(content.canReadCircle(10L, null)).thenReturn(false);
        assertFalse(access.canRead(ref, metadata, null));
    }

    @Test
    void imageReferenceMustBeAnExactJsonArrayElementNotASubstring() {
        Post substring = post(1L, ref.canonical() + "-extra");
        when(posts.selectList(any())).thenReturn(List.of(substring));
        when(content.canRead(substring, null)).thenReturn(true);
        assertFalse(access.canRead(ref, metadata, null));
        @SuppressWarnings("unchecked") ArgumentCaptor<Wrapper<Post>> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(posts).selectList(query.capture());
        assertTrue(query.getValue().getSqlSegment().contains("JSON_CONTAINS"));
        assertFalse(query.getValue().getSqlSegment().contains("LIKE"));
        substring.setImages("{\"fake\":\"" + ref.canonical() + "\"}");
        assertFalse(access.canRead(ref, metadata, null));
    }

    @Test
    void requestAdviceStripsTicketsInNestedJsonWithoutDroppingPasswordsOrExternalUrls() throws Exception {
        String decorated = ref.canonical() + "?ticket=" + tickets.issue(ref, 1L);
        JsonNode body = json.createObjectNode().put("images", "[\"" + decorated + "\"]")
                .put("avatar", decorated).put("password", "keep-write-only-input")
                .put("plainJson", "[  \"https://external.invalid/image.png\"  ]")
                .put("external", "http://169.254.169.254/latest/meta-data");
        HttpInputMessage normalized = advice.beforeBodyRead(new MockHttpInputMessage(json.writeValueAsBytes(body)),
                null, Object.class, MappingJackson2HttpMessageConverter.class);
        JsonNode actual = json.readTree(normalized.getBody());
        assertEquals(ref.canonical(), actual.path("avatar").asText());
        assertEquals(ref.canonical(), json.readTree(actual.path("images").asText()).get(0).asText());
        assertEquals("keep-write-only-input", actual.path("password").asText());
        assertEquals(body.path("external"), actual.path("external"));
        assertEquals(body.path("plainJson"), actual.path("plainJson"));
    }

    @Test
    void requestAdviceRejectsUnreadableOrNonDelegableReferences() throws Exception {
        UserContext.set(new LoginUser(2L, "reader", 0));
        JsonNode body = json.createObjectNode().put("avatar", ref.canonical() + "?ticket=forged");
        assertThrows(BusinessException.class, () -> advice.beforeBodyRead(new MockHttpInputMessage(json.writeValueAsBytes(body)),
                null, Object.class, MappingJackson2HttpMessageConverter.class));
        when(messages.selectList(any())).thenReturn(List.of(message(ref.canonical())));
        assertThrows(BusinessException.class, () -> advice.beforeBodyRead(new MockHttpInputMessage(json.writeValueAsBytes(body)),
                null, Object.class, MappingJackson2HttpMessageConverter.class));
    }

    @Test
    void responseAdviceCopiesEntityAndSignsOnlyControlledUrlsForCurrentViewer() {
        User owner = user(1L);
        owner.setAvatar(ref.canonical());
        owner.setPassword("must-not-serialize");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        JsonNode output = (JsonNode) advice.beforeBodyWrite(Result.success(owner), null, MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class, null, httpResponse);
        httpResponse.close();
        String url = output.path("data").path("avatar").asText();
        assertEquals(1L, tickets.verify(ref, url.substring(url.indexOf("?ticket=") + 8)));
        assertFalse(output.path("data").has("password"));
        assertEquals(ref.canonical(), owner.getAvatar());
        assertTrue(response.getHeader("Cache-Control").contains("no-store"));
        JsonNode external = json.getNodeFactory().textNode("https://other.invalid/image.png");
        clearInvocations(storage);
        assertEquals(external, advice.rewrite(external, null, true, new HashMap<>(), 0));
        verifyNoInteractions(storage);
    }

    @Test
    void unreadableLegacyDirectLinkIsRedactedRatherThanReturned() {
        MediaAccessService denied = mock(MediaAccessService.class);
        String old = "https://images.invalid/media/2025/01/01/0123456789abcdef0123456789abcdef.png";
        when(denied.requireReadable(any(), isNull())).thenThrow(new BusinessException(ResultCode.FORBIDDEN));
        MediaBodyAdvice safe = new MediaBodyAdvice(json, locator, denied, tickets);
        assertEquals("", safe.rewrite(json.getNodeFactory().textNode(old), null, true, new HashMap<>(), 0).asText());
    }

    @Test
    void proxySupportsRangesAndDoesNotTrustStoredMime() throws Exception {
        when(storage.stat(ref)).thenReturn(new MinioUtil.Metadata(10, "text/html", 1L));
        when(storage.open(ref, 2, 4)).thenReturn(new ByteArrayInputStream(new byte[]{2, 3, 4, 5}));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/file/content/" + ref.token());
        request.addHeader("Range", "bytes=2-5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MediaContentService(locator, tickets, access, storage).serve(ref.token(), tickets.issue(ref, 1L), request, response);
        assertEquals(206, response.getStatus());
        assertEquals("bytes 2-5/10", response.getHeader("Content-Range"));
        assertEquals("image/png", response.getContentType());
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("private, no-store", response.getHeader("Cache-Control"));
        assertArrayEquals(new byte[]{2, 3, 4, 5}, response.getContentAsByteArray());
    }

    @Test
    void proxyRejectsMissingTicketAndRechecksBannedUserAndVisibility() throws Exception {
        MediaContentService proxy = new MediaContentService(locator, tickets, access, storage);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/file/content/" + ref.token());
        MockHttpServletResponse response = new MockHttpServletResponse();
        proxy.serve(ref.token(), null, request, response);
        assertEquals(403, response.getStatus());
        verify(storage, never()).stat(any());
        String ticket = tickets.issue(ref, 1L);
        User banned = user(1L);
        banned.setStatus(0);
        when(users.selectById(1L)).thenReturn(banned);
        proxy.serve(ref.token(), ticket, request, response);
        assertEquals(403, response.getStatus());
        when(users.selectById(1L)).thenReturn(user(1L));
        when(messages.selectList(any())).thenReturn(List.of(message(ref.canonical())));
        String readerTicket = tickets.issue(ref, 2L);
        // A previously signed ticket is not enough once the conversation reference disappears.
        when(messages.selectList(any())).thenReturn(List.of());
        proxy.serve(ref.token(), readerTicket, request, response);
        assertEquals(403, response.getStatus());
        verify(storage, never()).open(any(), anyLong(), anyLong());
    }

    @Test
    void mvcUploadResponseIsTicketizedAndProxyRequiresTicketEvenWithoutJwtInterceptor() throws Exception {
        when(storage.upload(any(), eq("image/png"), eq(1L))).thenReturn(ref.objectName());
        MediaContentService proxy = new MediaContentService(locator, tickets, access, storage);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FileController(files, proxy)).setControllerAdvice(advice).build();
        MockHttpServletResponse upload = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/file/upload").file(new MockMultipartFile("file", "image.png", "text/html", new byte[]{1})))
                .andReturn().getResponse();
        assertEquals(200, upload.getStatus());
        String url = json.readTree(upload.getContentAsString()).path("data").asText();
        assertTrue(url.startsWith(ref.canonical() + "?ticket=m1.1."));
        assertEquals(403, mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(ref.canonical())).andReturn().getResponse().getStatus());
        String ticket = url.substring(url.indexOf("?ticket=") + 8);
        MockHttpServletResponse range = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(ref.canonical()).param("ticket", ticket).header("Range", "bytes=20-30"))
                .andReturn().getResponse();
        assertEquals(416, range.getStatus());
        assertEquals("bytes */10", range.getHeader("Content-Range"));
        verify(storage, never()).open(any(), anyLong(), anyLong());
    }

    @ParameterizedTest
    @CsvSource({"photo.JPG,image/jpeg", "photo.jpeg,image/jpeg", "photo.png,image/png", "photo.gif,image/gif", "photo.webp,image/webp", "movie.mp4,video/mp4"})
    void originalMimeWhitelistIsPreservedAndClientMimeIgnored(String filename, String expectedType) throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getContentType()).thenReturn("text/html");
        when(file.getSize()).thenReturn(1L);
        when(storage.upload(file, expectedType, 1L)).thenReturn(ref.objectName());
        assertEquals(ref.canonical(), files.upload(file));
        verify(storage).upload(file, expectedType, 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"evil.svg", "evil.html", "evil.php", "evil.png.html", "no-extension", "evil.JPG%00.html"})
    void disallowedExtensionsNeverReachStorage(String filename) throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.getSize()).thenReturn(1L);
        assertEquals(1019, assertThrows(BusinessException.class, () -> files.upload(file)).getCode());
        verify(storage, never()).upload(any(), anyString(), anyLong());
    }

    @Test
    void exactTwentyMbLimitIsAcceptedButLargerAndEmptyFilesAreRejected() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getSize()).thenReturn(20L * 1024 * 1024);
        when(storage.upload(file, "image/png", 1L)).thenReturn(ref.objectName());
        assertEquals(ref.canonical(), files.upload(file));
        when(file.getSize()).thenReturn(20L * 1024 * 1024 + 1);
        assertEquals(1013, assertThrows(BusinessException.class, () -> files.upload(file)).getCode());
        when(file.isEmpty()).thenReturn(true);
        assertEquals(400, assertThrows(BusinessException.class, () -> files.upload(file)).getCode());
        verify(storage, times(1)).upload(any(), anyString(), anyLong());
    }
}
