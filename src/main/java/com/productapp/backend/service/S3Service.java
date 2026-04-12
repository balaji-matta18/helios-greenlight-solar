package com.productapp.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * Uploads a file to S3 and returns the public URL.
     * Filename is sanitized before use — client-provided names are untrusted.
     * Key format: uploads/{uuid}_{sanitized_filename}
     */
    public String uploadFile(MultipartFile file) throws IOException {

        String safeName = sanitizeFilename(file.getOriginalFilename());
        String key = "uploads/" + UUID.randomUUID() + "_" + safeName;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            String fileUrl = buildPublicUrl(key);
            log.info("File uploaded to S3: {}", fileUrl);

            return fileUrl;

        } catch (S3Exception e) {
            log.error("S3 upload failed for file {}: {}", safeName, e.getMessage());
            throw new IOException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from S3 by its full URL.
     * Extracts the key from the URL before deleting.
     */
    public void deleteFile(String fileUrl) {

        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("File deleted from S3: {}", key);

        } catch (S3Exception e) {
            // Log but don't throw — a failed delete shouldn't break the business flow
            log.error("S3 delete failed for URL {}: {}", fileUrl, e.getMessage());
        }
    }

    // ========================= PRIVATE HELPERS =========================

    /**
     * Sanitizes the client-provided filename:
     * - Strips path traversal sequences (../)
     * - Replaces any character that isn't alphanumeric, dot, hyphen, or underscore with _
     * - Falls back to "file" if the result is blank
     */
    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file";
        }
        // Strip path separators (e.g. ../../etc/passwd)
        String cleaned = StringUtils.cleanPath(originalFilename);
        // Keep only safe characters
        String safe = cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.isBlank() ? "file" : safe;
    }

    private String buildPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    private String extractKeyFromUrl(String fileUrl) {
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        return fileUrl.replace(prefix, "");
    }
}