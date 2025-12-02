package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.PermissionDenyException;
import com.example.thangcachep.movie_project_be.exceptions.UnauthorizedException;
import com.example.thangcachep.movie_project_be.models.request.CreateRoomRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.WatchTogetherRoomResponse;
import com.example.thangcachep.movie_project_be.services.IWatchTogetherService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/watch-together")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WatchTogetherController {

    private final IWatchTogetherService watchTogetherService;
    private final LocalizationUtils localizationUtils;

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
            throw new UnauthorizedException(localizationUtils.getLocalizedMessage(MessageKeys.UNAUTHORIZED));
        }

        WatchTogetherRoomResponse room = watchTogetherService.createRoom(request, currentUser.getId());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.WATCH_TOGETHER_ROOM_CREATE_SUCCESS);
        ApiResponse<WatchTogetherRoomResponse> response = ApiResponse.success(message, room);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<WatchTogetherRoomResponse>> getRoom(@PathVariable String roomId) {
        WatchTogetherRoomResponse room = watchTogetherService.getRoom(roomId);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.SUCCESS); // Hoặc tạo key mới
        ApiResponse<WatchTogetherRoomResponse> response = ApiResponse.success(message, room);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<WatchTogetherRoomResponse>>> getActiveRooms(
            @RequestParam(required = false) Long movieId) {
        List<WatchTogetherRoomResponse> rooms = watchTogetherService.getActiveRooms(movieId);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.WATCH_TOGETHER_ROOM_GET_ALL_SUCCESS);
        ApiResponse<List<WatchTogetherRoomResponse>> response = ApiResponse.success(message, rooms);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable String roomId) throws PermissionDenyException {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UnauthorizedException(localizationUtils.getLocalizedMessage(MessageKeys.UNAUTHORIZED));
        }

        watchTogetherService.deleteRoom(roomId, currentUser.getId());
        String message = localizationUtils.getLocalizedMessage(MessageKeys.SUCCESS); // Hoặc tạo key mới
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }
}

