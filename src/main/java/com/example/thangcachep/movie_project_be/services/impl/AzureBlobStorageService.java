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
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // 2. Lấy container client
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        // 3. Tạo blob client
        BlobClient blobClient = containerClient.getBlobClient(uniqueFileName);

        // 4. Upload file (không set metadata để tránh lỗi InvalidMetadata)
        // Azure Blob Storage metadata không cho phép dấu gạch ngang trong key
        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // 5. Set content type sau khi upload (dùng BlobHttpHeaders, không dùng metadata)
        if (file.getContentType() != null) {
            com.azure.storage.blob.models.BlobHttpHeaders headers = new com.azure.storage.blob.models.BlobHttpHeaders();
            headers.setContentType(file.getContentType());
            blobClient.setHttpHeaders(headers);
        }

        // 6. Trả về public URL
        String fileUrl = publicUrl + "/" + uniqueFileName;
        log.info("✅ Đã upload file: {} -> {}", uniqueFileName, fileUrl);
        return fileUrl;
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

