package com.example.thangcachep.movie_project_be.convert;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.models.dto.UserDTO;
import com.example.thangcachep.movie_project_be.models.request.StaffCreateRequest;
import com.example.thangcachep.movie_project_be.models.request.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserConvert {

    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public UserEntity convertToEntity (UserDTO userDTO){
        UserEntity userEntity = modelMapper.map(userDTO, UserEntity.class);
        BeanUtils.copyProperties(userDTO,userEntity,"retypePassword");
        return userEntity;
    }

    public UserEntity convertToEntity (StaffCreateRequest userDTO){
        UserEntity userEntity = modelMapper.map(userDTO, UserEntity.class);
        BeanUtils.copyProperties(userDTO,userEntity,"retypePassword");
        return userEntity;
    }

    public UserDTO convertToDto (UserEntity user){
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        return userDTO;
    }

    public UserEntity updateEntityFromDto(UserUpdateRequest userDTO, UserEntity userEntity, Long userId){
        UserEntity newUserEntity = new UserEntity();
        // Update user information based on the DTO
        newUserEntity.setUserId(userId);
        if (userDTO.getFullName() != null) {
            newUserEntity.setFullName(userDTO.getFullName());
        }
        if (userDTO.getPhoneNumber() != null) {
            newUserEntity.setPhoneNumber(userDTO.getPhoneNumber());
        }
        if (userDTO.getEmail() != null) {
            newUserEntity.setEmail(userDTO.getEmail());
        }

        if (userDTO.getImg() != null) {
            newUserEntity.setImg(userDTO.getImg());
        }

//        if(userDTO.getActive() != null){
//            newUserEntity.setActive((userDTO.getActive()));
//        }

        // Update the password if it is provided in the DTO
        if (userDTO.getPassword() != null
                && !userDTO.getPassword().isEmpty()) {
            String newPassword = userDTO.getPassword();
            String encodedPassword = passwordEncoder.encode(newPassword);
            newUserEntity.setPassword(encodedPassword);
        }
        newUserEntity.setActive(userEntity.getActive());
        newUserEntity.setRole(userEntity.getRole());
        return newUserEntity;
    }

//    Page<AllUserResponse> convertEntitytoDto(Page<UserEntity> userEntityPage){
//        Page<AllUserResponse> data = modelMapper.map(userEntityPage, Page<AllUserResponse>.class);
//    }

}
