package com.example.thangcachep.movie_project_be.services.impl;

import com.example.thangcachep.movie_project_be.config.ModelMapperConfig;
import com.example.thangcachep.movie_project_be.entities.RoomTypeEntity;
import com.example.thangcachep.movie_project_be.models.dto.RoomTypeDTO;
import com.example.thangcachep.movie_project_be.repositories.RoomTypeRepository;
import com.example.thangcachep.movie_project_be.services.IRoomTypeService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomTypeService implements IRoomTypeService {


    private final ModelMapper modelMapper;
    private final RoomTypeRepository roomTypeRepository;

    @Override
    public RoomTypeDTO createRoomType(RoomTypeDTO roomTypeDTO) {
        RoomTypeEntity  roomTypeEntity = modelMapper.map(roomTypeDTO, RoomTypeEntity.class);
        RoomTypeEntity roomType = roomTypeRepository.save(roomTypeEntity);
        return modelMapper.map(roomType, RoomTypeDTO.class);
    }
}
