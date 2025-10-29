package com.example.thangcachep.movie_project_be.services;

import com.example.thangcachep.movie_project_be.entities.ReviewEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.ReviewDTO;
import com.example.thangcachep.movie_project_be.models.request.ReviewRequest;
import com.example.thangcachep.movie_project_be.models.responses.AllReviewResponse;
import com.example.thangcachep.movie_project_be.models.responses.AllUserResponse;
import com.example.thangcachep.movie_project_be.models.responses.ReviewResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IReviewService {

    ReviewEntity createReview(ReviewRequest reviewRequest) throws DataNotFoundException;
    Page<AllReviewResponse> getAllReviews(int page, int limit);
    List<ReviewDTO> getReviewById(Long id) throws DataNotFoundException;
}
