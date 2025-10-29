package com.example.thangcachep.movie_project_be.repositories;

import com.example.thangcachep.movie_project_be.entities.ManagementCustomerEntity;
import com.example.thangcachep.movie_project_be.entities.ManagementRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManagementCustomerRepository extends JpaRepository<ManagementCustomerEntity, Long> {

    // "SELECT * FROM management_rooms WHERE staff.userId = :staffId"
    List<ManagementCustomerEntity> findByStaff_UserId(Long staffId); // tim kiem boi staff trong class UserEntity , gach duoi la lui vao truong cua userentity
}
