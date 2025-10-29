package com.example.thangcachep.movie_project_be.services.impl;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.entities.ReplyEntity;
import com.example.thangcachep.movie_project_be.entities.ReviewEntity;
import com.example.thangcachep.movie_project_be.entities.UserEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.ReplyDTO;
import com.example.thangcachep.movie_project_be.repositories.ReplyRepository;
import com.example.thangcachep.movie_project_be.repositories.ReviewRepository;
import com.example.thangcachep.movie_project_be.repositories.UserRepository;
import com.example.thangcachep.movie_project_be.services.IReplyService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReplyService implements IReplyService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final LocalizationUtils localizationUtils;
    private final ReplyRepository replyRepository;

    @Override
    public ReplyEntity createReply(ReplyDTO replyDTO) throws DataNotFoundException {
        Optional<ReviewEntity> optionalReview = reviewRepository.findById(replyDTO.getReviewId());
        if(optionalReview.isEmpty()){
            throw new DataNotFoundException("not find review");
        }
        ReviewEntity review = optionalReview.get();

        Optional<UserEntity> optionalUser = userRepository.findByUserId(replyDTO.getStaffId());
        if(optionalUser.isEmpty()){
            throw new DataNotFoundException("not find staff");
        }
        UserEntity staff = optionalUser.get();
        ReplyEntity replyEntity = new ReplyEntity();
        replyEntity.setContent(replyDTO.getContent());
        replyEntity.setReview(review);
        replyEntity.setStaff(staff);
        return replyRepository.save(replyEntity);
    }
}
