package com.example.thangcachep.movie_project_be.controllers;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.ReplyEntity;
import com.example.thangcachep.movie_project_be.entities.RoomEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.*;
import com.example.thangcachep.movie_project_be.models.responses.*;
import com.example.thangcachep.movie_project_be.services.FileStorageService;
import com.example.thangcachep.movie_project_be.services.impl.*;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/staff")
@RequiredArgsConstructor

public class StaffController {

    private final CustomerService customerService;
    private final UserService userService;
    private final LocalizationUtils localizationUtils;
    private final RoomService roomService;
    private final ReviewService reviewService;
    private final ReplyService replyService;
    private final RoomTypeService roomTypeService;
    private final FileStorageService fileStorageService;


//==================================CUSTOMER========================================


    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDTO>> getMyCustomers(Authentication authentication) {
        UserEntity userDetails = (UserEntity) authentication.getPrincipal();
        Long staffId = userDetails.getUserId();

        List<CustomerDTO> customers = customerService.getCustomersByStaff(staffId);
        return ResponseEntity.ok(customers);
    }

    @PutMapping("/customers/active/{id}")
    public ResponseEntity<UpdateCustomerResponse> updateCustomerActive(
            @PathVariable Long id,
            @Valid @RequestParam("active") boolean active
    ) {
        UpdateCustomerResponse updateCustomerResponse = new UpdateCustomerResponse();
        customerService.updateCustomerActive(id, active);
        updateCustomerResponse.setMessage("update customer active successfully");
        return ResponseEntity.ok(updateCustomerResponse);
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<?> getCustomerById(
            @PathVariable("id") Long customerId
    ) throws DataNotFoundException {
        try {
            CustomerDTO existingCustomer = customerService.getCustomerById(customerId);
            return ResponseEntity.ok(existingCustomer);
        } catch (DataNotFoundException ex) {
            return ResponseEntity.badRequest().body("Not find customer with id = " + customerId);
        }

    }

    @PutMapping(value = "/customers/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCustomer(
            @PathVariable Long id,
            @Valid @ModelAttribute CustomerDTO customerDTO,
            BindingResult result
    ) throws IOException {
        List<MultipartFile> files = customerDTO.getFiles();
        files = files == null ? new ArrayList<MultipartFile>() : files;
        for (MultipartFile file : files) {
            if (file.getSize() == 0) {
                continue;
            }
            // Kiểm tra kích thước file và định dạng
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body("file is too large! Maximum size is 10MB");
            }
            String contenType = file.getContentType();
            if (contenType == null || !contenType.startsWith("image/")) {
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
        UpdateCustomerResponse updateCustomerResponse = new UpdateCustomerResponse();
        customerService.updateCustomer(id, customerDTO);
        updateCustomerResponse.setMessage("update customer successfully");
        return ResponseEntity.ok(updateCustomerResponse);
    }


    @DeleteMapping("/customers/{id}")
    public ResponseEntity<String> deleteCustomer(
            @PathVariable Long id
    ) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok("delete sucessfully");
    }


//==================================ROOM========================================


    @PostMapping(value = "/rooms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createRoom(
            @Valid @ModelAttribute RoomDTO roomDTO,
            BindingResult result

    ) throws IOException {
        RoomResponse roomResponse = new RoomResponse();
        if (result.hasErrors()) {
            List<String> errorMessages = result.getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .toList();
            roomResponse.setMessage(localizationUtils.getLocalizedMessage(MessageKeys.INSERT_ROOM_FAILED));
            roomResponse.setErrors(errorMessages);
            return ResponseEntity.badRequest().body(roomResponse);
        }
        List<MultipartFile> files = roomDTO.getFiles();
        files = files == null ? new ArrayList<MultipartFile>() : files;
        for (MultipartFile file : files) {
            if (file.getSize() == 0) {
                continue;
            }
            // Kiểm tra kích thước file và định dạng
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body("file is too large! Maximum size is 10MB");
            }
            String contenType = file.getContentType();
            if (contenType == null || !contenType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body("File must be an image");
            }
            // Lưu file và cập nhật thumbnail trong DTO
            String filename = fileStorageService.storeFile(file);
            //lưu vào đối tượng room trong DB => sẽ làm sau
            String imagePath = fileStorageService.getFileUrl(filename);
            roomDTO.setImage(imagePath);
            //lưu vào bảng room_images
        }
        RoomEntity room = roomService.createRoom(roomDTO);
        roomResponse.setMessage(localizationUtils.getLocalizedMessage(MessageKeys.INSERT_ROOM_SUCCESSFULLY));
        roomResponse.setRoomDTO(room);
        return ResponseEntity.ok(roomResponse);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<RoomDTO>> getMyRooms(Authentication authentication) {
        UserEntity userDetails = (UserEntity) authentication.getPrincipal();
        Long staffId = userDetails.getUserId();

        List<RoomDTO> rooms = roomService.getRoomsByStaff(staffId);
        return ResponseEntity.ok(rooms);
    }


    @PutMapping(value = "/rooms/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateRoom(
            @PathVariable Long id,
            @Valid @ModelAttribute RoomDTO roomDTO
    ) throws IOException {
        List<MultipartFile> files = roomDTO.getFiles();
        files = files == null ? new ArrayList<MultipartFile>() : files;
        for (MultipartFile file : files) {
            if (file.getSize() == 0) {
                continue;
            }
            // Kiểm tra kích thước file và định dạng
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body("file is too large! Maximum size is 10MB");
            }
            String contenType = file.getContentType();
            if (contenType == null || !contenType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body("File must be an image");
            }
            // Lưu file và cập nhật thumbnail trong DTO
            String filename = fileStorageService.storeFile(file);
            //lưu vào đối tượng room trong DB => sẽ làm sau
            String imagePath = fileStorageService.getFileUrl(filename);
            roomDTO.setImage(imagePath);
            //lưu vào bảng room_images
        }
        UpdateRoomResponse updateRoomResponse = new UpdateRoomResponse();
        roomService.updateRoom(id, roomDTO);
//        updateRoomResponse.setMessage("update room successfully");
//        return ResponseEntity.ok(updateRoomResponse);
        return ResponseEntity.ok("update room successfully");
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<String> deleteRoom(
            @PathVariable Long id
    ) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok("delete sucessfully");
    }


    //==================================USER========================================


    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.ok("delete sucessfully");
    }

    @GetMapping("/users/all-users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<AllUserResponse> listUsers = userService.getAllUsers(page, limit);

        return ResponseEntity.ok(listUsers);
    }


    //==================================REVIEW========================================

    @GetMapping("/reviews/all-reviews")
    public ResponseEntity<?> getAllReview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<AllReviewResponse> listReviews = reviewService.getAllReviews(page, limit);

        return ResponseEntity.ok(listReviews);
    }


    //==================================REPLY========================================


    @PostMapping("/reply/{reviewId}")
    public  ResponseEntity<?> createReply(
            @PathVariable Long reviewId,
            @RequestBody ReplyDTO replyDTO,
            Authentication authentication
            )throws DataNotFoundException{

        try {
            UserEntity user = (UserEntity) authentication.getPrincipal();
            Long staffId = user.getUserId();
            replyDTO.setReviewId(reviewId);
            replyDTO.setStaffId(staffId);
            ReplyEntity replyEntity = replyService.createReply(replyDTO);
            ReplyResponse replyResponse = new ReplyResponse();
            replyResponse.setMessage("create riview reply successfully");
            return ResponseEntity.ok(replyResponse);
        }catch(DataNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        }catch (Exception e){
            return ResponseEntity.badRequest().body("failded");
        }
    }

    //==================================RoomType========================================

    @PostMapping("/roomtype")
    public ResponseEntity<RoomTypeDTO> createRoomType(
            @RequestBody RoomTypeDTO roomTypeDTO
            ){
        return ResponseEntity.ok().body(roomTypeService.createRoomType(roomTypeDTO));
    }

    //==================================Price========================================
    @GetMapping("/price")
    public ResponseEntity<List<ManagePriceResponse>> getAllRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<ManagePriceResponse> p = roomService.getAllPriceRooms(page, limit);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(p.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(p.getTotalPages()))
                .header("X-Page", String.valueOf(page))
                .header("X-Size", String.valueOf(limit))
                .body(p.getContent());              // chỉ trả danh sách
    }
}