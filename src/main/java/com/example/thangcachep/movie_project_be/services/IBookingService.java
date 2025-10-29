package com.example.thangcachep.movie_project_be.services;

import com.example.thangcachep.movie_project_be.entities.BookingEntity;
import com.example.thangcachep.movie_project_be.models.request.BookingInforRequest;
import com.example.thangcachep.movie_project_be.models.responses.BookingResponse;

public interface IBookingService {

    BookingResponse createBooking(BookingInforRequest bookingInforRequest) throws Exception;

}
