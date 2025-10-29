package com.example.thangcachep.movie_project_be.convert;

import com.example.thangcachep.movie_project_be.entities.CustomerEntity;
import com.example.thangcachep.movie_project_be.models.dto.CustomerDTO;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CustomerConvert {

    private final ModelMapper modelMapper;

    public CustomerEntity convertToEntity(CustomerDTO customerDTO){
        CustomerEntity customerEntity = modelMapper.map(customerDTO, CustomerEntity.class);
        return  customerEntity;
    }

    public  CustomerDTO convertToDto(CustomerEntity customerEntity){
        CustomerDTO customerDTO = modelMapper.map(customerEntity, CustomerDTO.class);
        return  customerDTO;
    }

    public CustomerEntity updateToEntityFromDTO(CustomerDTO customerDTO, CustomerEntity customerEntity, Long customerid){
        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerId(customerid);
        if(customerDTO.getName() != null ){
            customer.setName(customerDTO.getName());
        }
        if(customerDTO.getEmail() != null ){
            customer.setEmail(customerDTO.getEmail());
        }
        if(customerDTO.getPhone() != null ){
            customer.setPhone(customerDTO.getPhone());
        }
        if(customerDTO.getNote() != null ){
            customer.setNote(customerDTO.getNote());
        }
        if(customerDTO.getImg() != null ){
            customer.setImg(customerDTO.getImg());
        }
        if(customerDTO.getAddress() != null ){
            customer.setAddress(customerDTO.getAddress());
        }

        return customer;
    }
//    public RoomDetailResponse convertToDtoResponseDetail(RoomEntity roomEntity){
//        RoomDetailResponse roomDTO = modelMapper.map(roomEntity, RoomDetailResponse.class);
//        return  roomDTO;
//    }

//    public Page<RoomDTO> convertToPageDto(Page<RoomEntity> roomEntity){
//        Page<RoomDTO> roomDTO = modelMapper.map(roomEntity, roomEntity.cl)
//        return  roomDTO;
//    }
}
