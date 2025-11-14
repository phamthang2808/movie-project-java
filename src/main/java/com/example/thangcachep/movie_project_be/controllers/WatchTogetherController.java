package com.example.thangcachep.movie_project_be.controllers;

import com.example.thangcachep.movie_project_be.exceptions.PermissionDenyException;
import com.example.thangcachep.movie_project_be.models.request.CreateRoomRequest;
import com.example.thangcachep.movie_project_be.models.responses.WatchTogetherRoomResponse;
import com.example.thangcachep.movie_project_be.services.IWatchTogetherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watch-together")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WatchTogetherController {

    private final IWatchTogetherService watchTogetherService;

    private com.example.thangcachep.movie_project_be.entities.UserEntity getCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof com.example.thangcachep.movie_project_be.entities.UserEntity) {
            return (com.example.thangcachep.movie_project_be.entities.UserEntity) authentication.getPrincipal();
        }
        return null;
    }

    @PostMapping("/rooms")
    public ResponseEntity<WatchTogetherRoomResponse> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {
        com.example.thangcachep.movie_project_be.entities.UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        WatchTogetherRoomResponse room = watchTogetherService.createRoom(request, currentUser.getId());
        return ResponseEntity.ok(room);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<WatchTogetherRoomResponse> getRoom(@PathVariable String roomId) {
        WatchTogetherRoomResponse room = watchTogetherService.getRoom(roomId);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<WatchTogetherRoomResponse>> getActiveRooms(
            @RequestParam(required = false) Long movieId) {
        List<WatchTogetherRoomResponse> rooms = watchTogetherService.getActiveRooms(movieId);
        return ResponseEntity.ok(rooms);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String roomId) throws PermissionDenyException {
        com.example.thangcachep.movie_project_be.entities.UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        watchTogetherService.deleteRoom(roomId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}

