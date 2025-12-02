//package com.example.thangcachep.movie_project_be.controllers;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.example.thangcachep.movie_project_be.services.impl.BucketService;
//
///**
// * AWS S3 Bucket Controller
// * Xử lý các request upload/delete file trên AWS S3
// */
//@RestController
//@RequestMapping("/api/v1/upload")
//@CrossOrigin(origins = "*") // Cho phép React gọi
//public class BucketController {
//
//    @Autowired
//    private BucketService bucketService;
//
//    /**
//     * Upload video file lên AWS S3
//     * POST /api/v1/upload/video
//     */
//    @PostMapping("/video")
//    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
//        // Kiểm tra file rỗng
//        if (file.isEmpty()) {
//            return ResponseEntity.badRequest().body("File không được rỗng");
//        }
//
//        try {
//            // 1. Gọi service để upload lên AWS S3
//            String fileUrl = bucketService.uploadFile(file);
//
//            // 2. Trả URL về cho React
//            Map<String, String> response = new HashMap<>();
//            response.put("videoUrl", fileUrl);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            e.printStackTrace(); // In lỗi ra console để debug
//            return ResponseEntity.status(500).body("Upload thất bại: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Upload ảnh (poster, backdrop, avatar, etc.) lên AWS S3
//     * POST /api/v1/upload/single hoặc /api/v1/upload/image
//     * Cũng có thể upload file Word và các file khác (không giới hạn)
//     */
//    @PostMapping("/single")
//    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return ResponseEntity.badRequest().body("File không được rỗng");
//        }
//
//        try {
//            // Upload lên AWS S3 - chấp nhận mọi loại file (ảnh, video, document, etc.)
//            String fileUrl = bucketService.uploadFile(file);
//
//            // Trả URL về
//            Map<String, Object> response = new HashMap<>();
//            response.put("url", fileUrl);
//            response.put("fileUrl", fileUrl);
//            response.put("fileName", file.getOriginalFilename());
//            response.put("fileType", file.getContentType());
//            response.put("fileSize", file.getSize());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Upload thất bại: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Alias cho upload ảnh
//     */
//    @PostMapping("/image")
//    public ResponseEntity<?> uploadImageAlias(@RequestParam("file") MultipartFile file) {
//        return uploadImage(file);
//    }
//
//    /**
//     * Upload document file (Word, PDF, Excel, etc.) lên AWS S3
//     * POST /api/v1/upload/document
//     */
//    @PostMapping("/document")
//    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return ResponseEntity.badRequest().body("File không được rỗng");
//        }
//
//        try {
//            // Validate file type - chỉ chấp nhận document files
//            String contentType = file.getContentType();
//            String originalFilename = file.getOriginalFilename();
//            String fileExtension = "";
//
//            if (originalFilename != null && originalFilename.contains(".")) {
//                fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
//            }
//
//            // Danh sách các file extension được phép
//            java.util.List<String> allowedExtensions = java.util.Arrays.asList(
//                    ".doc", ".docx", ".pdf", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".rtf"
//            );
//
//            // Kiểm tra extension
//            if (!allowedExtensions.contains(fileExtension)) {
//                return ResponseEntity.badRequest().body(
//                        "File không được hỗ trợ. Chỉ chấp nhận: " + String.join(", ", allowedExtensions)
//                );
//            }
//
//            // Upload lên AWS S3
//            String fileUrl = bucketService.uploadFile(file);
//
//            // Trả URL về
//            Map<String, Object> response = new HashMap<>();
//            response.put("url", fileUrl);
//            response.put("fileUrl", fileUrl);
//            response.put("fileName", originalFilename);
//            response.put("fileType", contentType);
//            response.put("fileSize", file.getSize());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Upload thất bại: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Upload bất kỳ file nào lên AWS S3 (không giới hạn loại file)
//     * POST /api/v1/upload/file
//     */
//    @PostMapping("/file")
//    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return ResponseEntity.badRequest().body("File không được rỗng");
//        }
//
//        try {
//            // Upload lên AWS S3 - chấp nhận mọi loại file
//            String fileUrl = bucketService.uploadFile(file);
//
//            // Trả URL về
//            Map<String, Object> response = new HashMap<>();
//            response.put("url", fileUrl);
//            response.put("fileUrl", fileUrl);
//            response.put("fileName", file.getOriginalFilename());
//            response.put("fileType", file.getContentType());
//            response.put("fileSize", file.getSize());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Upload thất bại: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Upload multiple files lên AWS S3
//     * POST /api/v1/upload/multiple
//     */
//    @PostMapping("/multiple")
//    public ResponseEntity<?> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
//        if (files == null || files.length == 0) {
//            return ResponseEntity.badRequest().body("Không có file nào được chọn");
//        }
//
//        try {
//            Map<String, Object> response = new HashMap<>();
//            java.util.List<String> urls = new java.util.ArrayList<>();
//
//            for (MultipartFile file : files) {
//                if (!file.isEmpty()) {
//                    String fileUrl = bucketService.uploadFile(file);
//                    urls.add(fileUrl);
//                }
//            }
//
//            response.put("urls", urls);
//            response.put("count", urls.size());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Upload thất bại: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Xóa file khỏi AWS S3
//     * DELETE /api/v1/upload/delete
//     */
//    @DeleteMapping("/delete")
//    public ResponseEntity<?> deleteFile(@RequestParam("fileUrl") String fileUrl) {
//        try {
//            boolean deleted = bucketService.deleteFile(fileUrl);
//
//            if (deleted) {
//                Map<String, String> response = new HashMap<>();
//                response.put("message", "Xóa file thành công");
//                return ResponseEntity.ok(response);
//            } else {
//                return ResponseEntity.status(500).body("Xóa file thất bại");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Xóa file thất bại: " + e.getMessage());
//        }
//    }
//}
//
//
