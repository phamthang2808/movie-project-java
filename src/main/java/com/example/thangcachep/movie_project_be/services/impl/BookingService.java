package com.example.thangcachep.movie_project_be.services.impl;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.BookingEntity;
import com.example.thangcachep.movie_project_be.entities.CustomerEntity;
import com.example.thangcachep.movie_project_be.entities.RoomEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.responses.BookingResponse;
import com.example.thangcachep.movie_project_be.repositories.BookingRepository;
import com.example.thangcachep.movie_project_be.repositories.CustomerRepository;
import com.example.thangcachep.movie_project_be.repositories.RoomRepository;
import com.example.thangcachep.movie_project_be.models.request.BookingInforRequest;
import com.example.thangcachep.movie_project_be.services.IBookingService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@RequiredArgsConstructor
@Service
public class BookingService implements IBookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final LocalizationUtils localizationUtils;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingInforRequest bookingInforRequest) throws DataNotFoundException {

        CustomerEntity customer = customerRepository.findByEmail(bookingInforRequest.getEmail())
                .orElseGet(() ->{
                    CustomerEntity newCustomer = new CustomerEntity();
                    newCustomer.setName(bookingInforRequest.getName());
                    newCustomer.setEmail(bookingInforRequest.getEmail());
                    newCustomer.setPhone(bookingInforRequest.getPhone());
                    return customerRepository.save(newCustomer);
                        });
        if (bookingInforRequest.getCheckinDate() == null || bookingInforRequest.getCheckoutDate() == null
                || !bookingInforRequest.getCheckinDate().isBefore(bookingInforRequest.getCheckoutDate())) {
            throw new IllegalArgumentException("Ngày checkin/checkout không hợp lệ");
        }
        Optional<RoomEntity> optionalRoom = roomRepository.findByRoomId(bookingInforRequest.getRoomId());
        if(optionalRoom.isEmpty()){
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.FIND_ROOM_FAILED));
        }
        RoomEntity existingRoom = optionalRoom.get();
        BookingEntity bookingEntity = new BookingEntity();
        bookingEntity.setRoom(existingRoom);
        bookingEntity.setCustomer(customer);
        bookingEntity.setStatus("booked");
        bookingEntity.setCheckinDate(bookingInforRequest.getCheckinDate());
        bookingEntity.setCheckoutDate(bookingInforRequest.getCheckoutDate());
        BookingEntity saveBookings = bookingRepository.save(bookingEntity);
        BookingResponse response = BookingResponse.builder()
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .name(customer.getName())
                .roomId(saveBookings.getRoom().getRoomId())
                .status(saveBookings.getStatus())
                .checkinDate(saveBookings.getCheckinDate())
                .checkoutDate(saveBookings.getCheckoutDate())
                .build();
        return response;
    }
}
