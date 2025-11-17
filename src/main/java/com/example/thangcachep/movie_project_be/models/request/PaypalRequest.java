package com.example.thangcachep.movie_project_be.models.request;

import lombok.Data;

import lombok.*;

@Getter
@Setter
public class PaypalRequest {
    private String total;
    private String currency = "USD";
}