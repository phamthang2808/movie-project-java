package com.example.thangcachep.movie_project_be.services.impl;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure Blob Storage Service
 * Thay thế AWS S3 BucketService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureBlobStorageService {

    private final BlobServiceClient blobServiceClient;

    @Value("${azure.storage.container-name}")
    private String containerName;

    @Value("${azure.storage.public-url}")
    private String publicUrl;

    /**
     * Upload file lên Azure Blob Storage và trả về URL công khai
     *
     * @param file File cần upload
     * @return URL công khai của file
     * @throws IOException Nếu có lỗi khi upload
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 1. Tạo tên file unique (để tránh trùng lặp)
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // 2. Lấy container client
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        // 3. Tạo blob client
        BlobClient blobClient = containerClient.getBlobClient(uniqueFileName);

        // 4. Upload file (không set metadata để tránh lỗi InvalidMetadata)
        // Azure Blob Storage metadata không cho phép dấu gạch ngang trong key
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // 5. Set headers sau khi upload (dùng BlobHttpHeaders, không dùng metadata)
        com.azure.storage.blob.models.BlobHttpHeaders headers = new com.azure.storage.blob.models.BlobHttpHeaders();

        // 5.1. Detect và set Content-Type đúng cho video files
        String contentType = file.getContentType();
        if (contentType == null || contentType.equals("application/octet-stream")) {
            // Nếu không có Content-Type hoặc là octet-stream, detect từ extension
            contentType = detectContentType(fileExtension, originalFilename);
        }
        headers.setContentType(contentType);

        // 5.2. Set headers cho video streaming (quan trọng cho video lớn)
        if (isVideoFile(fileExtension)) {
            // Accept-Ranges: bytes - Cho phép Range Request (cần thiết cho video streaming)
            // Cache-Control: public, max-age=31536000 - Cache 1 năm cho video
            headers.setCacheControl("public, max-age=31536000, immutable");
        } else if (isImageFile(fileExtension)) {
            // Cache headers cho ảnh
            headers.setCacheControl("public, max-age=31536000, immutable");
        } else {
            // Cache headers cho các file khác
            headers.setCacheControl("public, max-age=86400");
        }

        blobClient.setHttpHeaders(headers);

        // 6. Trả về public URL
        String fileUrl = publicUrl + "/" + uniqueFileName;
        log.info("✅ Đã upload file: {} -> {} (Content-Type: {})", uniqueFileName, fileUrl, contentType);
        return fileUrl;
    }

    /**
     * Detect Content-Type từ file extension
     */
    private String detectContentType(String fileExtension, String originalFilename) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            return "application/octet-stream";
        }

        switch (fileExtension.toLowerCase()) {
            // Video formats
            case ".mp4":
                return "video/mp4";
            case ".webm":
                return "video/webm";
            case ".mkv":
                return "video/x-matroska";
            case ".avi":
                return "video/x-msvideo";
            case ".mov":
                return "video/quicktime";
            case ".flv":
                return "video/x-flv";
            case ".m3u8":
                return "application/vnd.apple.mpegurl";
            case ".ts":
                return "video/mp2t";

            // Image formats
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            case ".svg":
                return "image/svg+xml";

            // Document formats
            case ".pdf":
                return "application/pdf";
            case ".doc":
                return "application/msword";
            case ".docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls":
                return "application/vnd.ms-excel";
            case ".xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            default:
                return "application/octet-stream";
        }
    }

    /**
     * Kiểm tra xem file có phải là video không
     */
    private boolean isVideoFile(String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            return false;
        }
        String ext = fileExtension.toLowerCase();
        return ext.equals(".mp4") || ext.equals(".webm") || ext.equals(".mkv")
                || ext.equals(".avi") || ext.equals(".mov") || ext.equals(".flv")
                || ext.equals(".m3u8") || ext.equals(".ts");
    }

    /**
     * Kiểm tra xem file có phải là ảnh không
     */
    private boolean isImageFile(String fileExtension) {
        if (fileExtension == null || fileExtension.isEmpty()) {
            return false;
        }
        String ext = fileExtension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")
                || ext.equals(".gif") || ext.equals(".webp") || ext.equals(".svg");
    }

    /**
     * Xóa file khỏi Azure Blob Storage (Synchronous)
     *
     * @param fileUrl URL của file cần xóa
     * @return true nếu xóa thành công
     */
    public boolean deleteFile(String fileUrl) {
        try {
            // Extract file name from URL
            // URL format: https://account.blob.core.windows.net/container/filename
            String fileName = fileUrl.substring(publicUrl.length() + 1);

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            blobClient.delete();
            log.info("✅ Đã xóa file: {}", fileName);
            return true;
        } catch (Exception e) {
            log.error("❌ Lỗi khi xóa file: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * Xóa file khỏi Azure Blob Storage (Async - không block HTTP request)
     *
     * @param fileUrl URL của file cần xóa
     * @return CompletableFuture<Boolean> true nếu xóa thành công
     */
    @Async("fileExecutor")
    public CompletableFuture<Boolean> deleteFileAsync(String fileUrl) {
        return CompletableFuture.completedFuture(deleteFile(fileUrl));
    }
}

