package com.example.thangcachep.movie_project_be.controllers;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.request.StaffCreateRequest;
import com.example.thangcachep.movie_project_be.models.responses.AllStaffResponse;
import com.example.thangcachep.movie_project_be.models.responses.StaffResponse;
import com.example.thangcachep.movie_project_be.services.FileStorageService;
import com.example.thangcachep.movie_project_be.services.impl.UserService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admins")
@RequiredArgsConstructor
public class AdminController {

    public final UserService userService;
    public final LocalizationUtils localizationUtils;
    private final FileStorageService fileStorageService;

    @GetMapping("/users/all-staff")
    public ResponseEntity<List<AllStaffResponse>> getAllStaffs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<AllStaffResponse> p = userService.getAllStaffs(page, limit);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(p.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(p.getTotalPages()))
                .header("X-Page", String.valueOf(page))
                .header("X-Size", String.valueOf(limit))
                .body(p.getContent());              // chỉ trả danh sách
    }

    @PostMapping(value = "/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createStaff(
            @Valid @ModelAttribute StaffCreateRequest staffCreateRequest,
            BindingResult result
    ) throws IOException {
        StaffResponse staffResponse = new StaffResponse();
        if(result.hasErrors()){
            List<String> errorMessage = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .toList();
            staffResponse.setMessage(localizationUtils.getLocalizedMessage(MessageKeys.INSERT_STAFF_FAILED));
            staffResponse.setErrors(errorMessage);
            return ResponseEntity.badRequest().body(staffResponse);
        }
        List<MultipartFile> files = staffCreateRequest.getFiles();
        files = files == null ? new ArrayList<MultipartFile>() : files;
        for (MultipartFile file : files){
            if(file.getSize() == 0){
                continue;
            }
            // Kiểm tra kích thước file và định dạng
            if(file.getSize() > 10 * 1024 * 1024){
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body("file is too large! Maximum size is 10MB");
            }
            String contenType = file.getContentType();
            if(contenType == null || !contenType.startsWith("image/")){
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body("File must be an image");
            }
            // Lưu file và cập nhật thumbnail trong DTO
            String filename = fileStorageService.storeFile(file);
            //lưu vào đối tượng room trong DB => sẽ làm sau
            String imagePath = fileStorageService.getFileUrl(filename);
            staffCreateRequest.setImg(imagePath);
        }
        UserEntity userEntity = userService.createStaff(staffCreateRequest);
        staffResponse.setMessage(localizationUtils.getLocalizedMessage(MessageKeys.INSERT_STAFF_SUCCESSFULLY));
        staffResponse.setUserEntity(userEntity);
        return ResponseEntity.ok(staffResponse);
    }


}
