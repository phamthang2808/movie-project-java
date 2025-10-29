package com.example.thangcachep.movie_project_be.services.impl;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.CustomerEntity;
import com.example.thangcachep.movie_project_be.entities.ReviewEntity;
import com.example.thangcachep.movie_project_be.entities.RoomEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.ReviewDTO;
import com.example.thangcachep.movie_project_be.models.request.ReviewRequest;
import com.example.thangcachep.movie_project_be.models.responses.AllReviewResponse;
import com.example.thangcachep.movie_project_be.models.responses.AllUserResponse;
import com.example.thangcachep.movie_project_be.models.responses.ReviewResponse;
import com.example.thangcachep.movie_project_be.repositories.CustomerRepository;
import com.example.thangcachep.movie_project_be.repositories.ReviewRepository;
import com.example.thangcachep.movie_project_be.repositories.RoomRepository;
import com.example.thangcachep.movie_project_be.services.IReviewService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final RoomRepository roomRepository;
    private final LocalizationUtils localizationUtils;

    public ReviewEntity createReview(ReviewRequest reviewRequest) throws DataNotFoundException {
        CustomerEntity customer = new CustomerEntity();
        customer.setName(reviewRequest.getName());
        customerRepository.save(customer);

        Optional<RoomEntity> optionnalRoom = roomRepository.findByRoomId(reviewRequest.getRoomId());
        if(optionnalRoom.isEmpty()){
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.FIND_ROOM_FAILED));
        }
        RoomEntity existingRoom = optionnalRoom.get();

        ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setRoom(existingRoom);
        reviewEntity.setCustomer(customer);
        reviewEntity.setRating(reviewRequest.getRating());
        reviewEntity.setComment(reviewRequest.getComment());
        return reviewRepository.save(reviewEntity);
    }

    @Override
    public Page<AllReviewResponse> getAllReviews(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<ReviewEntity> data = reviewRepository.findAll(pageable);
        return data.map(AllReviewResponse::new);
    }

    @Override
    public List<ReviewDTO> getReviewById(Long id) throws DataNotFoundException {
        List<ReviewDTO> reviewDTOS = new ArrayList<>();
        Optional<List<RoomEntity>> optionalRoom = roomRepository.findAllByRoomId(id);
        if(optionalRoom.isEmpty() || optionalRoom.get().isEmpty()){
            throw new DataNotFoundException(localizationUtils.getLocalizedMessage(MessageKeys.FIND_ROOM_FAILED));
        }
        List<RoomEntity> existingRooms = optionalRoom.get();
        for(RoomEntity roomEntities:  existingRooms){
            List<ReviewEntity> existingReviews = reviewRepository.findAllByRoom_RoomId(roomEntities.getRoomId());
            for(ReviewEntity reviewDTO: existingReviews){
                ReviewDTO review = new ReviewDTO(reviewDTO);
                reviewDTOS.add(review);
            }

        }
        return reviewDTOS;
    }
}
