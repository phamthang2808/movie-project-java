package com.example.thangcachep.movie_project_be.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.exceptions.InvalidParamException;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.services.impl.AzureBlobStorageService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;

/**
 * Azure Blob Storage Controller
 * Xử lý các request upload/delete file trên Azure Blob Storage
 */
@RestController
@RequestMapping("/api/v1/upload")
@CrossOrigin(origins = "*") // Cho phép React gọi
@RequiredArgsConstructor
public class AzureBlobStorageController {

    private final AzureBlobStorageService azureBlobStorageService;
    private final LocalizationUtils localizationUtils;

    /**
     * Upload video file lên Azure Blob Storage
     * POST /api/v1/upload/video
     */
    @PostMapping("/video")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {
        // Kiểm tra file rỗng
        if (file.isEmpty()) {
            throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_FILE_EMPTY));
        }

        // 1. Gọi service để upload lên Azure Blob Storage
        String fileUrl = azureBlobStorageService.uploadFile(file);

        // 2. Trả URL về cho React
        Map<String, Object> data = new HashMap<>();
        data.put("videoUrl", fileUrl);

        String message = localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_VIDEO_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload ảnh (poster, backdrop, avatar, etc.) lên Azure Blob Storage
     * POST /api/v1/upload/single hoặc /api/v1/upload/image
     * Cũng có thể upload file Word và các file khác (không giới hạn)
     */
    @PostMapping("/single")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_FILE_EMPTY));
        }

        // Upload lên Azure Blob Storage - chấp nhận mọi loại file (ảnh, video, document, etc.)
        String fileUrl = azureBlobStorageService.uploadFile(file);

        // Trả URL về
        Map<String, Object> data = new HashMap<>();
        data.put("url", fileUrl);
        data.put("fileUrl", fileUrl);
        data.put("fileName", file.getOriginalFilename());
        data.put("fileType", file.getContentType());
        data.put("fileSize", file.getSize());

        String message = localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_IMAGE_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Alias cho upload ảnh
     */
    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImageAlias(@RequestParam("file") MultipartFile file) throws IOException {
        return uploadImage(file);
    }

    /**
     * Upload document file (Word, PDF, Excel, etc.) lên Azure Blob Storage
     * POST /api/v1/upload/document
     */
    @PostMapping("/document")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_FILE_EMPTY));
        }

        // Validate file type - chỉ chấp nhận document files
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // Danh sách các file extension được phép
        java.util.List<String> allowedExtensions = java.util.Arrays.asList(
                ".doc", ".docx", ".pdf", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".rtf"
        );

        // Kiểm tra extension
        if (!allowedExtensions.contains(fileExtension)) {
            throw new InvalidParamException(
                    localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_FILE_INVALID_EXTENSION, String.join(", ", allowedExtensions))
            );
        }

        // Upload lên Azure Blob Storage
        String fileUrl = azureBlobStorageService.uploadFile(file);

        // Trả URL về
        Map<String, Object> data = new HashMap<>();
        data.put("url", fileUrl);
        data.put("fileUrl", fileUrl);
        data.put("fileName", originalFilename);
        data.put("fileType", contentType);
        data.put("fileSize", file.getSize());

        String message = localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_DOCUMENT_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload bất kỳ file nào lên Azure Blob Storage (không giới hạn loại file)
     * POST /api/v1/upload/file
     */
    @PostMapping("/file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_FILE_EMPTY));
        }

        // Upload lên Azure Blob Storage - chấp nhận mọi loại file
        String fileUrl = azureBlobStorageService.uploadFile(file);

        // Trả URL về
        Map<String, Object> data = new HashMap<>();
        data.put("url", fileUrl);
        data.put("fileUrl", fileUrl);
        data.put("fileName", file.getOriginalFilename());
        data.put("fileType", file.getContentType());
        data.put("fileSize", file.getSize());

        String message = localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_FILE_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload multiple files lên Azure Blob Storage
     * POST /api/v1/upload/multiple
     */
    @PostMapping("/multiple")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_FILE_EMPTY));
        }

        Map<String, Object> data = new HashMap<>();
        java.util.List<String> urls = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileUrl = azureBlobStorageService.uploadFile(file);
                urls.add(fileUrl);
            }
        }

        data.put("urls", urls);
        data.put("count", urls.size());

        String message = localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_MULTIPLE_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }

    /**
     * Xóa file khỏi Azure Blob Storage
     * DELETE /api/v1/upload/delete
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteFile(@RequestParam("fileUrl") String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new InvalidParamException(localizationUtils.getLocalizedMessage(MessageKeys.INVALID_PARAM));
        }

        boolean deleted = azureBlobStorageService.deleteFile(fileUrl);

        if (!deleted) {
            throw new RuntimeException(localizationUtils.getLocalizedMessage(MessageKeys.RUNTIME_ERROR));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deleted", true);
        data.put("fileUrl", fileUrl);

        String message = localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_DELETE_SUCCESS);
        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, data);
        return ResponseEntity.ok(response);
    }
}
