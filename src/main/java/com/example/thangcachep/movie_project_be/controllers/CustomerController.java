package com.example.thangcachep.movie_project_be.controllers;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.CustomerEntity;
import com.example.thangcachep.movie_project_be.models.dto.CustomerDTO;
import com.example.thangcachep.movie_project_be.models.responses.CustomerResponse;
import com.example.thangcachep.movie_project_be.services.FileStorageService;
import com.example.thangcachep.movie_project_be.services.impl.CustomerService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("${api.prefix}/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final LocalizationUtils localizationUtils;
    private final CustomerService customerService;
    private final FileStorageService fileStorageService;

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCustomer(
            @Valid @ModelAttribute CustomerDTO customerDTO,
            BindingResult result
            ) throws IOException {
        CustomerResponse customerResponse = new CustomerResponse();
        if(result.hasErrors()){
            List<String> errorMessage = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .toList();
            customerResponse.setMessage(localizationUtils.getLocalizedMessage(MessageKeys.INSERT_CUSTOMER_FAILED));
            customerResponse.setErrors(errorMessage);
            return ResponseEntity.badRequest().body(customerResponse);
        }
        List<MultipartFile> files = customerDTO.getFiles();
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
            customerDTO.setImg(imagePath);
            //lưu vào bảng room_images
        }
        CustomerEntity customer = customerService.createCustomer(customerDTO);
        customerResponse.setMessage(localizationUtils.getLocalizedMessage(MessageKeys.INSERT_CUSTOMER_SUCCESSFULLY));
        customerResponse.setCustomerEntity(customer);
        return ResponseEntity.ok(customerResponse);
    }





}
