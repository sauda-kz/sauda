package com.sauda.integration.storage;

import com.sauda.config.StorageProperties;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@Profile({"dev", "prod"})
public class S3ObjectStorageProvider implements ObjectStorageProvider {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorageProvider(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.bucket();
    }

    @Override
    public void putObject(String key, InputStream content, long contentLength, String contentType) {
        ensureBucketExists();
        log.info(
                "[STORAGE] Uploading object: bucket={}, key={}, size={}",
                bucket,
                key,
                contentLength);
        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(contentLength)
                        .build();
        s3Client.putObject(request, RequestBody.fromInputStream(content, contentLength));
        log.debug("[STORAGE] Upload completed: key={}", key);
    }

    @Override
    public StoredObject getObject(String key) {
        log.info("[STORAGE] Downloading object: bucket={}, key={}", bucket, key);
        try {
            var response =
                    s3Client.getObject(
                            GetObjectRequest.builder().bucket(bucket).key(key).build(),
                            ResponseTransformer.toBytes());
            String contentType =
                    response.response().contentType() != null
                            ? response.response().contentType()
                            : "application/octet-stream";
            return new StoredObject(
                    response.asInputStream(), response.response().contentLength(), contentType);
        } catch (NoSuchKeyException exception) {
            throw new IllegalStateException("Stored object not found: " + key, exception);
        }
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.debug("[STORAGE] Bucket exists: {}", bucket);
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw exception;
            }
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("[STORAGE] Created bucket: {}", bucket);
        }
    }
}
