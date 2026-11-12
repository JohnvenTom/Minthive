package com.minthive.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minthive.config.MinioConfig;
import com.minthive.media.MediaLocator.Ref;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Storage access is restricted to configured buckets. Never changes a bucket policy. */
@Component
@RequiredArgsConstructor
public class MinioUtil {
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private static final ObjectMapper JSON = new ObjectMapper();

    public record Metadata(long size, String contentType, Long ownerId) { }

    /** Content-Type is supplied by the server whitelist, never by the multipart client. */
    public String upload(MultipartFile file, String contentType, Long ownerId) throws Exception {
        if (ownerId == null || ownerId <= 0) throw new IllegalArgumentException("Media owner required");
        ensureBucket();
        String filename = Objects.requireNonNull(file.getOriginalFilename());
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String name = "users/" + ownerId + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try (InputStream input = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder().bucket(privateBucket()).object(name)
                    .stream(input, file.getSize(), -1).contentType(contentType)
                    .userMetadata(Map.of("owner-id", ownerId.toString())).build());
        }
        return name;
    }

    public Metadata stat(Ref ref) throws Exception {
        if (!ref.legacy()) assertPrivateBucket();
        StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucket(ref)).object(ref.objectName()).build());
        Long owner = null;
        if (!ref.legacy()) {
            String recordedOwner = stat.userMetadata().get("owner-id");
            String pathOwner = ref.objectName().split("/")[1];
            if (!pathOwner.equals(recordedOwner)) throw new IllegalStateException("Media ownership metadata mismatch");
            owner = Long.valueOf(recordedOwner);
            if (owner <= 0) throw new IllegalStateException("Invalid media owner");
        }
        return new Metadata(stat.size(), stat.contentType(), owner);
    }

    public InputStream open(Ref ref, long offset, long length) throws Exception {
        if (!ref.legacy()) assertPrivateBucket();
        return minioClient.getObject(GetObjectArgs.builder().bucket(bucket(ref)).object(ref.objectName())
                .offset(offset).length(length).build());
    }

    /** Caller must authorize against stat metadata first. Legacy objects deliberately cannot be deleted. */
    public void delete(Ref ref) throws Exception {
        if (ref.legacy()) throw new IllegalArgumentException("Legacy ownership is unknown");
        assertPrivateBucket();
        minioClient.removeObject(RemoveObjectArgs.builder().bucket(privateBucket()).object(ref.objectName()).build());
    }

    private String privateBucket() { return minioConfig.getBucket() + "-private"; }
    private String bucket(Ref ref) { return ref.legacy() ? minioConfig.getBucket() : privateBucket(); }

    private void ensureBucket() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(privateBucket()).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(privateBucket()).build());
        }
        assertPrivateBucket();
    }

    /** Fail closed: this dedicated bucket must not have Allow policies, even conditional/public-prefix ones. */
    private void assertPrivateBucket() throws Exception {
        String policy;
        try {
            policy = minioClient.getBucketPolicy(GetBucketPolicyArgs.builder().bucket(privateBucket()).build());
        } catch (ErrorResponseException e) {
            if ("NoSuchBucketPolicy".equals(e.errorResponse().code())) return;
            throw e;
        }
        if (policy == null || policy.isBlank()) return;
        JsonNode root = JSON.readTree(policy);
        JsonNode statements = root.get("Statement");
        if (statements == null || !(statements.isArray() || statements.isObject())) {
            throw new IllegalStateException("Cannot verify private media bucket policy");
        }
        if (statements.isObject()) checkDenyOnly(statements);
        else for (JsonNode statement : statements) checkDenyOnly(statement);
    }

    private void checkDenyOnly(JsonNode statement) {
        if (!"Deny".equals(statement.path("Effect").asText())) {
            throw new IllegalStateException("Private media bucket has an Allow policy; operator intervention required");
        }
    }
}
