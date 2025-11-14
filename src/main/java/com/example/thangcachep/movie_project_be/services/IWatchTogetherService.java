package com.example.thangcachep.movie_project_be.services;

import com.example.thangcachep.movie_project_be.exceptions.PermissionDenyException;
import com.example.thangcachep.movie_project_be.models.request.CreateRoomRequest;
import com.example.thangcachep.movie_project_be.models.responses.WatchTogetherRoomResponse;

import java.util.List;

public interface IWatchTogetherService {
    WatchTogetherRoomResponse createRoom(CreateRoomRequest request, Long userId);
    WatchTogetherRoomResponse getRoom(String roomId);
    List<WatchTogetherRoomResponse> getActiveRooms(Long movieId);
    void deleteRoom(String roomId, Long userId) throws PermissionDenyException;
    void addUserToRoom(String roomId, Long userId);
    void removeUserFromRoom(String roomId, Long userId);
    Long getRoomUserCount(String roomId);
}

