package com.example.thangcachep.movie_project_be.convert;

import com.example.thangcachep.movie_project_be.entities.RoomEntity;
import com.example.thangcachep.movie_project_be.models.dto.RoomDTO;
import com.example.thangcachep.movie_project_be.models.responses.RoomDetailResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RoomConvert {

    private final ModelMapper modelMapper;

    public RoomEntity convertToEntity(RoomDTO roomDTO){
        RoomEntity roomEntity = modelMapper.map(roomDTO, RoomEntity.class);
        return  roomEntity;
    }

    public  RoomDTO convertToDto(RoomEntity roomEntity){
        RoomDTO roomDTO = modelMapper.map(roomEntity, RoomDTO.class);
        return  roomDTO;
    }

    public RoomDetailResponse convertToDtoResponseDetail(RoomEntity roomEntity){
        RoomDetailResponse roomDTO = modelMapper.map(roomEntity, RoomDetailResponse.class);
        return  roomDTO;
    }

    public RoomEntity updateToEntityFromDTO(RoomDTO roomDTO, RoomEntity roomEntity, Long roomId){
        RoomEntity room = new RoomEntity();
        room.setRoomId(roomId);

        if(roomDTO.getName() != null ){
            room.setName(roomDTO.getName());
        }

        if(roomDTO.getImage() != null ) {
            room.setImage(roomDTO.getImage());
        }

        if(roomDTO.getAddress() != null ){
            room.setAddress(roomDTO.getAddress());
        }

        if(roomDTO.getDescription() != null ) {
            room.setDescription(roomDTO.getDescription());
        }

        if(roomDTO.getTitle() != null ){
            room.setTitle(roomDTO.getTitle());
        }

        if(roomDTO.getGuests() != null ) {
            room.setGuests(roomDTO.getGuests());
        }

        if(roomDTO.getSize() != null ){
            room.setSize(roomDTO.getSize());
        }

        if(roomDTO.getBeds() != null ) {
            room.setBeds(roomDTO.getBeds());
        }

        if(roomDTO.getView() != null ){
            room.setView(roomDTO.getView());
        }

        if(roomDTO.getPrice() != null ) {
            room.setPrice(roomDTO.getPrice());
        }

        if(roomDTO.getDiscount() != null ){
            room.setDiscount(roomDTO.getDiscount());
        }

        if(roomDTO.getAirConditioning() != null ) {
            room.setAirConditioning(roomDTO.getAirConditioning());
        }

        if(roomDTO.getWifi() != null ){
            room.setWifi(roomDTO.getWifi());
        }

        if(roomDTO.getHairDryer() != null ) {
            room.setHairDryer(roomDTO.getHairDryer());
        }

        if(roomDTO.getPetsAllowed() != null ){
            room.setPetsAllowed(roomDTO.getPetsAllowed());
        }

        if(roomDTO.getNonSmoking() != null ) {
            room.setNonSmoking(roomDTO.getNonSmoking());
        }
        return room;
    }

//    public Page<RoomDTO> convertToPageDto(Page<RoomEntity> roomEntity){
//        Page<RoomDTO> roomDTO = modelMapper.map(roomEntity, roomEntity.cl)
//        return  roomDTO;
//    }
}
