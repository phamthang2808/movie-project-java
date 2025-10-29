package com.example.thangcachep.movie_project_be.services;

import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.UserDTO;
import com.example.thangcachep.movie_project_be.models.request.StaffCreateRequest;
import com.example.thangcachep.movie_project_be.models.request.UserUpdateRequest;
import com.example.thangcachep.movie_project_be.models.responses.AllStaffResponse;
import com.example.thangcachep.movie_project_be.models.responses.AllUserResponse;
import org.springframework.data.domain.Page;

public interface IUserService {
    UserEntity createUser(UserDTO userDTO) throws Exception;
//    String login(String phoneNumber, String password, Long roleId) throws Exception;
    String login(String phoneNumber, String password) throws Exception;
    UserEntity getUserDetailsFromToken(String token) throws Exception;
    UserDTO getUserById(Long userId) throws DataNotFoundException;
    void updateUser(Long userId, UserUpdateRequest userDTO) throws DataNotFoundException;
    Page<AllStaffResponse> getAllStaffs(int page, int limit);
    UserEntity createStaff(StaffCreateRequest staffCreateRequest);
    void deleteUser(long id);
    Page<AllUserResponse> getAllUsers(int page, int limit);
}
