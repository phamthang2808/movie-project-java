package com.example.thangcachep.movie_project_be.controllers;

import com.example.thangcachep.movie_project_be.entities.ReviewEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.ReviewDTO;
import com.example.thangcachep.movie_project_be.models.request.ReviewRequest;
import com.example.thangcachep.movie_project_be.models.responses.AllReviewResponse;
import com.example.thangcachep.movie_project_be.models.responses.AllUserResponse;
import com.example.thangcachep.movie_project_be.models.responses.ReviewResponse;
import com.example.thangcachep.movie_project_be.services.impl.ReviewService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{id}")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long id,
            @RequestBody ReviewRequest reviewRequest
    ) throws DataNotFoundException {
        ReviewResponse reviewResponse = new ReviewResponse();
        reviewRequest.setRoomId(id);
        ReviewEntity reviewEntity = reviewService.createReview(reviewRequest);
        reviewResponse.setMessage("post review successfully");
        return ResponseEntity.ok(reviewResponse);
    }


    @GetMapping("/{roomId}")
    public ResponseEntity<?> getReviewById(
            @PathVariable Long roomId
    ) throws DataNotFoundException{
        try {
            List<ReviewDTO> reviewDTOs = new ArrayList<>();
            reviewDTOs = reviewService.getReviewById(roomId);
            return ResponseEntity.ok(reviewDTOs);
        }catch (DataNotFoundException e){
            return ResponseEntity.badRequest().body("Not find room with id = "+ roomId);
        }
    }


}
