package com.example.thangcachep.movie_project_be.services.impl;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * AWS S3 Bucket Service
 * Xử lý upload và quản lý file trên AWS S3
 * Sử dụng @Async cho delete operations để không block HTTP request
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BucketService {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.public-url}")
    private String publicUrl;

    /**
     * Upload file lên AWS S3 và trả về URL công khai
     *
     * @param file File cần upload
     * @return URL công khai của file
     * @throws IOException Nếu có lỗi khi upload
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 1. Tạo một tên file duy nhất (để tránh trùng lặp)
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // 2. Chuẩn bị yêu cầu Upload với metadata tối ưu
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFileName) // Tên file trên S3
                .contentType(file.getContentType()); // Báo cho S3 biết đây là file gì (video/mp4, image/jpeg...)

        // Tối ưu cho video files - Sử dụng cacheControl() thay vì metadata()
        if (file.getContentType() != null && file.getContentType().startsWith("video/")) {
            // Set cache control cho video (S3 sẽ tự động set header này)
            requestBuilder.cacheControl("public, max-age=31536000, immutable");
        }

        PutObjectRequest putObjectRequest = requestBuilder.build();

        // 3. Gửi file (dưới dạng InputStream) lên AWS S3
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()
        ));

        // 4. Trả về URL công khai (Public URL)
        // Ví dụ: https://bucket-name.s3.region.amazonaws.com/filename
        // hoặc: https://your-cdn-domain.com/filename (nếu dùng CloudFront)
        return publicUrl + "/" + uniqueFileName;
    }

    /**
     * Xóa file khỏi AWS S3 (Synchronous - dùng khi cần kết quả ngay)
     *
     * @param fileUrl URL của file cần xóa
     * @return true nếu xóa thành công
     */
    public boolean deleteFile(String fileUrl) {
        try {
            // Extract file key from URL
            // URL format: https://bucket-name.s3.region.amazonaws.com/filename
            // hoặc: https://your-cdn-domain.com/filename
            String fileKey = fileUrl.substring(publicUrl.length() + 1);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("✅ Đã xóa file: {}", fileKey);
            return true;
        } catch (Exception e) {
            log.error("❌ Lỗi khi xóa file: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * Xóa file khỏi AWS S3 (Async - không block HTTP request)
     * Dùng khi không cần chờ kết quả ngay
     *
     * @param fileUrl URL của file cần xóa
     * @return CompletableFuture<Boolean> true nếu xóa thành công
     */
    @Async("fileExecutor")
    public CompletableFuture<Boolean> deleteFileAsync(String fileUrl) {
        try {
            String fileKey = fileUrl.substring(publicUrl.length() + 1);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("✅ Đã xóa file async: {}", fileKey);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("❌ Lỗi khi xóa file async: {}", fileUrl, e);
            return CompletableFuture.completedFuture(false);
        }
    }


}


