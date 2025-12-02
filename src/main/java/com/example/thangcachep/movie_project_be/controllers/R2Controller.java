//package com.example.thangcachep.movie_project_be.controllers; // <-- Sửa lại package cho đúng
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.example.thangcachep.movie_project_be.services.impl.R2FileStorageService;
//
//@RestController
//@RequestMapping("/api/v1/upload")
//@CrossOrigin(origins = "*") // Cho phép React gọi
//public class R2Controller {
//
//    @Autowired
//    private R2FileStorageService fileStorageService;
//
//    @PostMapping("/video")
//    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
//
//        // Kiểm tra file rỗng
//        if (file.isEmpty()) {
//            return ResponseEntity.badRequest().body("File không được rỗng");
//        }
//
//        try {
//            // 1. Gọi service để upload
//            String fileUrl = fileStorageService.uploadFile(file);
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
//     * Upload ảnh (poster, backdrop, avatar, etc.)
//     * POST /api/v1/upload/single hoặc /api/v1/upload/image
//     */
//    @PostMapping("/single")
//    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty()) {
//            return ResponseEntity.badRequest().body("File không được rỗng");
//        }
//
//        try {
//            // Upload lên R2
//            String fileUrl = fileStorageService.uploadFile(file);
//
//            // Trả URL về
//            Map<String, String> response = new HashMap<>();
//            response.put("url", fileUrl);
//            response.put("fileUrl", fileUrl);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Upload thất bại: " + e.getMessage());
//        }
//    }
//
//}