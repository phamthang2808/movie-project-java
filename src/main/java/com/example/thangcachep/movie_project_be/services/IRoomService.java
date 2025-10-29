package com.example.thangcachep.movie_project_be.services;

import com.example.thangcachep.movie_project_be.entities.RoomEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.RoomAllDTO;
import com.example.thangcachep.movie_project_be.models.dto.RoomDTO;
import com.example.thangcachep.movie_project_be.models.request.RoomSearchRequest;
import com.example.thangcachep.movie_project_be.models.responses.ManagePriceResponse;
import com.example.thangcachep.movie_project_be.models.responses.RoomDetailResponse;
import org.springframework.data.domain.Page;


import java.util.List;

public interface IRoomService {
    Page<RoomAllDTO> getAllRooms(int page, int limit);
    Page<ManagePriceResponse> getAllPriceRooms(int page, int limit);
    Page<RoomAllDTO> searchRooms(RoomSearchRequest roomSearchRequest);
    RoomEntity createRoom(RoomDTO roomDTO);
    List<RoomDTO> getRoomsByStaff(Long staffId);
    RoomDetailResponse getRoomById(Long RoomId) throws DataNotFoundException;
    RoomEntity updateRoom(long id, RoomDTO roomDTO);
    void deleteRoom(long id);
}
