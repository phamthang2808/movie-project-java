package com.example.thangcachep.movie_project_be.controllers;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.RoomAllDTO;
import com.example.thangcachep.movie_project_be.models.request.RoomSearchRequest;
import com.example.thangcachep.movie_project_be.models.responses.RoomDetailResponse;
import com.example.thangcachep.movie_project_be.services.impl.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final LocalizationUtils localizationUtils;

    @GetMapping("/search")
    public ResponseEntity<List<RoomAllDTO>> searchRooms(
         @ModelAttribute RoomSearchRequest roomSearchRequest
    ) {
        roomSearchRequest.normalizeDefaults();  //set default cho page
        Page<RoomAllDTO> p = roomService.searchRooms(roomSearchRequest);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(p.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(p.getTotalPages()))
                .header("X-Page", String.valueOf(roomSearchRequest.getPage()))
                .header("X-Size", String.valueOf(roomSearchRequest.getLimit()))
                .body(p.getContent());              // chỉ trả danh sách

    }

    @GetMapping("")
    public ResponseEntity<List<RoomAllDTO>> getAllRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Page<RoomAllDTO> p = roomService.getAllRooms(page, limit);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(p.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(p.getTotalPages()))
                .header("X-Page", String.valueOf(page))
                .header("X-Size", String.valueOf(limit))
                .body(p.getContent());              // chỉ trả danh sách
    }




    @GetMapping("/{id}")
    public ResponseEntity<?> getRoomById(
            @PathVariable("id") Long roomId
    ) throws DataNotFoundException {
        try {
            RoomDetailResponse existingRoom = roomService.getRoomById(roomId);
            return ResponseEntity.ok(existingRoom);
        }catch (DataNotFoundException ex){
            return ResponseEntity.badRequest().body("Not find room with id = "+ roomId);
        }

    }



}
