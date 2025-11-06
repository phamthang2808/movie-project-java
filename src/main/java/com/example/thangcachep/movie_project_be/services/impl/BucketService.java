package com.example.thangcachep.movie_project_be.services.impl;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * AWS S3 Bucket Service
 * Xử lý upload và quản lý file trên AWS S3
 */
@Service
@RequiredArgsConstructor
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

        // 2. Chuẩn bị yêu cầu Upload
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFileName) // Tên file trên S3
                .contentType(file.getContentType()) // Báo cho S3 biết đây là file gì (video/mp4, image/jpeg...)
                .build();

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
     * Xóa file khỏi AWS S3
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
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // DEPRECATED: Local Storage (Đã chuyển sang S3)
    // ==========================================
    // Method uploadAvatarToLocal() đã được thay thế bằng uploadFile() (S3)
    // Giữ lại code này để reference, có thể xóa sau khi migrate xong

    // @Autowired(required = false)
    // private FileUploadProperties fileUploadProperties;

    /**
     * @deprecated Đã chuyển sang dùng uploadFile() để upload lên AWS S3
     * Upload avatar vào thư mục uploads local và trả về URL
     * (KHÔNG CÒN DÙNG - Đã chuyển sang S3)
     */
    /*
    public String uploadAvatarToLocal(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        String filename = originalFilename != null ? StringUtils.cleanPath(originalFilename) : "";
        String fileExtension = "";
        if (filename != null && !filename.isEmpty() && filename.contains(".")) {
            fileExtension = filename.substring(filename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        Path uploadDir;
        if (fileUploadProperties != null && fileUploadProperties.getUploadDir() != null) {
            uploadDir = Paths.get(fileUploadProperties.getUploadDir(), "avatars");
        } else {
            uploadDir = Paths.get("uploads", "avatars");
        }

        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path destination = uploadDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/avatars/" + uniqueFilename;
    }
    */
}


