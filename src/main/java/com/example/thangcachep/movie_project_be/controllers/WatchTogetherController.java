package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.PermissionDenyException;
import com.example.thangcachep.movie_project_be.models.request.CreateRoomRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.WatchTogetherRoomResponse;
import com.example.thangcachep.movie_project_be.services.IWatchTogetherService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/watch-together")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WatchTogetherController {

    private final IWatchTogetherService watchTogetherService;

    private UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserEntity) {
            return (UserEntity) authentication.getPrincipal();
        }
        return null;
    }

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<WatchTogetherRoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            ApiResponse<WatchTogetherRoomResponse> response = ApiResponse.error("Bạn phải đăng nhập để tạo phòng", 401);
            return ResponseEntity.status(401).body(response);
        }

        WatchTogetherRoomResponse room = watchTogetherService.createRoom(request, currentUser.getId());
        ApiResponse<WatchTogetherRoomResponse> response = ApiResponse.success("Tạo phòng xem cùng thành công", room);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<WatchTogetherRoomResponse>> getRoom(@PathVariable String roomId) {
        WatchTogetherRoomResponse room = watchTogetherService.getRoom(roomId);
        ApiResponse<WatchTogetherRoomResponse> response = ApiResponse.success("Lấy thông tin phòng thành công", room);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<WatchTogetherRoomResponse>>> getActiveRooms(
            @RequestParam(required = false) Long movieId) {
        List<WatchTogetherRoomResponse> rooms = watchTogetherService.getActiveRooms(movieId);
        ApiResponse<List<WatchTogetherRoomResponse>> response = ApiResponse.success("Lấy danh sách phòng thành công", rooms);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable String roomId) throws PermissionDenyException {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            ApiResponse<Void> response = ApiResponse.error("Bạn phải đăng nhập để xóa phòng", 401);
            return ResponseEntity.status(401).body(response);
        }

        watchTogetherService.deleteRoom(roomId, currentUser.getId());
        ApiResponse<Void> response = ApiResponse.success("Xóa phòng thành công", null);
        return ResponseEntity.ok(response);
    }
}

