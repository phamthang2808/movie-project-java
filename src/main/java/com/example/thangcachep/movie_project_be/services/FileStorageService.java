package com.example.thangcachep.movie_project_be.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.thangcachep.movie_project_be.config.FileUploadProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileUploadProperties fileUploadProperties;

    /**
     * Lưu file vào thư mục upload và trả về tên file duy nhất
     */
    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        // Thêm UUID vào trước tên file để đảm bảo tên file là duy nhất
        String uniqueFilename = UUID.randomUUID().toString() + "_" + filename.toLowerCase();
        
        // Đường dẫn đến thư mục lưu file từ configuration
        Path uploadDir = Paths.get(fileUploadProperties.getUploadDir());
        
        // Kiểm tra và tạo thư mục nếu nó không tồn tại
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // Đường dẫn đầy đủ đến file
        Path destination = Paths.get(uploadDir.toString(), uniqueFilename);
        
        // Sao chép file vào thư mục đích
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        
        return uniqueFilename;
    }

    /**
     * Trả về đường dẫn đầy đủ cho URL của file
     */
    public String getFileUrl(String filename) {
        return "/" + fileUploadProperties.getUploadDir() + "/" + filename;
    }

    /**
     * Xóa file khỏi hệ thống
     */
    public boolean deleteFile(String filename) {
        try {
            Path filePath = Paths.get(fileUploadProperties.getUploadDir()).resolve(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }
}

