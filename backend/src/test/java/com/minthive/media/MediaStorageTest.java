package com.minthive.media;

import com.minthive.config.MinioConfig;
import com.minthive.util.MinioUtil;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaStorageTest {
    private final MinioClient client = mock(MinioClient.class);
    private final MinioConfig config = new MinioConfig();
    private MinioUtil storage;
    private MediaLocator locator;
    private final MockMultipartFile file = new MockMultipartFile("file", "photo.PNG", "text/html", new byte[]{1, 2, 3});

    @BeforeEach
    void setUp() {
        config.setBucket("development");
        config.setEndpoint("https://storage.invalid");
        locator = new MediaLocator(config);
        storage = new MinioUtil(client, config);
    }

    @Test
    void createsOnlyPrivateBucketAndPersistsOwnershipWithoutPublicPolicy() throws Exception {
        when(client.bucketExists(any())).thenReturn(false);
        when(client.getBucketPolicy(any())).thenReturn("");
        String object = storage.upload(file, "image/png", 42L);
        assertTrue(object.matches("users/42/[a-f0-9]{32}\\.png"));
        ArgumentCaptor<MakeBucketArgs> make = ArgumentCaptor.forClass(MakeBucketArgs.class);
        verify(client).makeBucket(make.capture());
        assertEquals("development-private", make.getValue().bucket());
        ArgumentCaptor<PutObjectArgs> put = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(put.capture());
        assertEquals("development-private", put.getValue().bucket());
        assertEquals("image/png", put.getValue().contentType());
        assertTrue(put.getValue().userMetadata().get("x-amz-meta-owner-id").contains("42"));
        verify(client, never()).setBucketPolicy(any());
    }

    @Test
    void existingPublicPrivateBucketIsRejectedNeverSilentlyReconfigured() throws Exception {
        when(client.bucketExists(any())).thenReturn(true);
        when(client.getBucketPolicy(any())).thenReturn("""
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*",
                "Action":"s3:GetObject","Resource":"arn:aws:s3:::development-private/*"}]}
                """);
        assertThrows(IllegalStateException.class, () -> storage.upload(file, "image/png", 1L));
        assertThrows(IllegalStateException.class, () -> storage.stat(locator.fromName("users/1/0123456789abcdef0123456789abcdef.png")));
        verify(client, never()).setBucketPolicy(any());
        verify(client, never()).putObject(any());
        verify(client, never()).makeBucket(any());
        verify(client, never()).statObject(any());
    }

    @Test
    void inabilityToInspectPolicyFailsClosed() throws Exception {
        when(client.bucketExists(any())).thenReturn(true);
        when(client.getBucketPolicy(any())).thenThrow(new IOException("policy access denied"));
        assertThrows(IOException.class, () -> storage.upload(file, "image/png", 1L));
        verify(client, never()).putObject(any());
        verify(client, never()).setBucketPolicy(any());
    }

    @Test
    void explicitNoSuchBucketPolicyMeansPrivate() throws Exception {
        when(client.bucketExists(any())).thenReturn(true);
        ErrorResponseException exception = mock(ErrorResponseException.class);
        ErrorResponse error = mock(ErrorResponse.class);
        when(error.code()).thenReturn("NoSuchBucketPolicy");
        when(exception.errorResponse()).thenReturn(error);
        when(client.getBucketPolicy(any())).thenThrow(exception);
        assertDoesNotThrow(() -> storage.upload(file, "image/png", 1L));
        verify(client).putObject(any());
        verify(client, never()).setBucketPolicy(any());
    }

    @Test
    void ownerIsTrustedOnlyWhenMetadataAndPathAgree() throws Exception {
        when(client.getBucketPolicy(any())).thenReturn("");
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(client.statObject(any())).thenReturn(stat);
        when(stat.size()).thenReturn(3L);
        when(stat.contentType()).thenReturn("image/png");
        MediaLocator.Ref ref = locator.fromName("users/42/0123456789abcdef0123456789abcdef.png");
        when(stat.userMetadata()).thenReturn(Map.of("owner-id", "42"));
        assertEquals(42L, storage.stat(ref).ownerId());
        when(stat.userMetadata()).thenReturn(Map.of("owner-id", "2"));
        assertThrows(IllegalStateException.class, () -> storage.stat(ref));
        when(stat.userMetadata()).thenReturn(Map.of());
        assertThrows(IllegalStateException.class, () -> storage.stat(ref));
    }

    @Test
    void legacyReadsNeverCreateOrChangeBucketsAndNeverInferOwnershipFromMetadata() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.userMetadata()).thenReturn(Map.of("owner-id", "1"));
        when(client.statObject(any())).thenReturn(stat);
        MediaLocator.Ref ref = locator.fromName("2024/01/01/0123456789abcdef0123456789abcdef.png");
        assertNull(storage.stat(ref).ownerId());
        ArgumentCaptor<StatObjectArgs> args = ArgumentCaptor.forClass(StatObjectArgs.class);
        verify(client).statObject(args.capture());
        assertEquals("development", args.getValue().bucket());
        assertThrows(IllegalArgumentException.class, () -> storage.delete(ref));
        verify(client, never()).getBucketPolicy(any());
        verify(client, never()).bucketExists(any());
        verify(client, never()).makeBucket(any());
        verify(client, never()).setBucketPolicy(any());
        verify(client, never()).removeObject(any());
    }

    @Test
    void storageDeletionExceptionsAreNotSwallowed() throws Exception {
        doThrow(new IOException("offline")).when(client).removeObject(any());
        MediaLocator.Ref ref = locator.fromName("users/42/0123456789abcdef0123456789abcdef.png");
        assertThrows(IOException.class, () -> storage.delete(ref));
    }

    @Test
    void parserAcceptsOnlyConfiguredBucketAndExactGeneratedObjectShapes() {
        MediaLocator.Ref ref = locator.fromName("users/42/0123456789abcdef0123456789abcdef.png");
        assertEquals(ref, locator.parse(ref.canonical() + "?ticket=temporary"));
        assertEquals(ref, locator.parse("https://storage.invalid/development-private/" + ref.objectName()));
        assertNull(locator.parse("https://storage.invalid.attacker.test/development-private/" + ref.objectName()));
        assertNull(locator.parse("https://storage.invalid/other-bucket/" + ref.objectName()));
        assertNull(locator.parse("http://169.254.169.254/latest/meta-data/"));
        assertNull(locator.parse("https://storage.invalid/development-private/../secret"));
        assertNull(locator.parse("https://storage.invalid/development-private/users%2F42%2Fsecret.png"));
        assertThrows(IllegalArgumentException.class, () -> locator.fromToken(ref.token() + "="));
        assertThrows(IllegalArgumentException.class, () -> locator.fromName("users/42/../index.html"));
    }
}
