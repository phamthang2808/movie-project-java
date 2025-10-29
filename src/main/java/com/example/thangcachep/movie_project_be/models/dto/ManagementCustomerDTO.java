package com.example.thangcachep.movie_project_be.models.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ManagementCustomerDTO {

    private UserDTO staffId;

    private CustomerDTO customerId;
}
