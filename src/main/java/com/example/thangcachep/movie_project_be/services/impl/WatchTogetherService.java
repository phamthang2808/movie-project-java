package com.example.thangcachep.movie_project_be.services.impl;

import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomEntity;
import com.example.thangcachep.movie_project_be.entities.WatchTogetherRoomUserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.exceptions.PermissionDenyException;
import com.example.thangcachep.movie_project_be.models.request.CreateRoomRequest;
import com.example.thangcachep.movie_project_be.models.responses.WatchTogetherRoomResponse;
import com.example.thangcachep.movie_project_be.repositories.MovieRepository;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.repositories.WatchTogetherRoomRepository;
import com.example.thangcachep.movie_project_be.repositories.WatchTogetherRoomUserRepository;
import com.example.thangcachep.movie_project_be.services.IWatchTogetherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchTogetherService implements IWatchTogetherService {

    private final WatchTogetherRoomRepository roomRepository;
    private final WatchTogetherRoomUserRepository roomUserRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public WatchTogetherRoomResponse createRoom(CreateRoomRequest request, Long userId) {
        // Get movie
        MovieEntity movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new DataNotFoundException("Movie not found with id: " + request.getMovieId()));

        // Get user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        // Create room
        WatchTogetherRoomEntity room = WatchTogetherRoomEntity.builder()
                .id(UUID.randomUUID().toString())
                .movie(movie)
                .name(request.getRoomName() != null && !request.getRoomName().trim().isEmpty()
                        ? request.getRoomName()
                        : "Room for " + movie.getTitle())
                .createdBy(user)
                .isActive(true)
                .build();

        room = roomRepository.save(room);

        // Add creator to room
        addUserToRoom(room.getId(), userId);

        return mapToResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public WatchTogetherRoomResponse getRoom(String roomId) {
        WatchTogetherRoomEntity room = roomRepository.findActiveRoomById(roomId)
                .orElseThrow(() -> new DataNotFoundException("Room not found with id: " + roomId));
        return mapToResponse(room);
    }

    @Override
    public List<WatchTogetherRoomResponse> getActiveRooms(Long movieId) {
        List<WatchTogetherRoomEntity> rooms = roomRepository.findActiveRooms(movieId);
        return rooms.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRoom(String roomId, Long userId) throws PermissionDenyException {
        WatchTogetherRoomEntity room = roomRepository.findActiveRoomById(roomId)
                .orElseThrow(() -> new DataNotFoundException("Room not found with id: " + roomId));

        // Check if user is the creator
        if (!room.getCreatedBy().getId().equals(userId)) {
            throw new PermissionDenyException("Only room creator can delete the room");
        }

        // Remove all users from room
        roomUserRepository.deleteByRoomId(roomId);

        // Deactivate room
        room.setIsActive(false);
        roomRepository.save(room);
    }

    @Override
    @Transactional
    public void addUserToRoom(String roomId, Long userId) {
        WatchTogetherRoomEntity room = roomRepository.findActiveRoomById(roomId)
                .orElseThrow(() -> new DataNotFoundException("Room not found with id: " + roomId));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + userId));

        // Check if user already in room
        if (roomUserRepository.findByRoomIdAndUserId(roomId, userId).isPresent()) {
            return; // User already in room
        }

        // Add user to room
        WatchTogetherRoomUserEntity roomUser = WatchTogetherRoomUserEntity.builder()
                .room(room)
                .user(user)
                .build();

        roomUserRepository.save(roomUser);
    }

    @Override
    @Transactional
    public void removeUserFromRoom(String roomId, Long userId) {
        roomUserRepository.findByRoomIdAndUserId(roomId, userId)
                .ifPresent(roomUserRepository::delete);
    }

    @Override
    public Long getRoomUserCount(String roomId) {
        return roomRepository.countUsersInRoom(roomId);
    }

    private WatchTogetherRoomResponse mapToResponse(WatchTogetherRoomEntity room) {
        Long userCount = roomRepository.countUsersInRoom(room.getId());

        return WatchTogetherRoomResponse.builder()
                .id(room.getId())
                .movieId(room.getMovie().getId())
                .movieTitle(room.getMovie().getTitle())
                .name(room.getName())
                .createdBy(room.getCreatedBy() != null ? room.getCreatedBy().getId() : null)
                .createdByName(room.getCreatedBy() != null ? room.getCreatedBy().getName() : null)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .userCount(userCount)
                .build();
    }
}

