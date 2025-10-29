package com.example.thangcachep.movie_project_be.services;

import com.example.thangcachep.movie_project_be.entities.CustomerEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.CustomerDTO;

import java.util.List;

public interface ICustomerService {

    CustomerEntity createCustomer(CustomerDTO customerDTO);
    List<CustomerDTO> getCustomersByStaff(Long staffId);
    CustomerDTO  getCustomerById(Long customerId) throws DataNotFoundException;
    CustomerEntity updateCustomer(long id, CustomerDTO customerDTO);
    CustomerEntity updateCustomerActive(long id,boolean active);
    void deleteCustomer(long id);
}
