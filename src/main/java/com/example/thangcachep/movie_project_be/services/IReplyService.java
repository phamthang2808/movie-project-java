package com.example.thangcachep.movie_project_be.services;

import com.example.thangcachep.movie_project_be.entities.ReplyEntity;
import com.example.thangcachep.movie_project_be.exceptions.DataNotFoundException;
import com.example.thangcachep.movie_project_be.models.dto.ReplyDTO;

public interface IReplyService {
    ReplyEntity createReply(ReplyDTO replyDTO) throws DataNotFoundException;
}
