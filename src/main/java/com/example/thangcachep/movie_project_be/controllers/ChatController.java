package com.example.thangcachep.movie_project_be.controllers;

import com.example.thangcachep.movie_project_be.models.request.ChatRequest;
import com.example.thangcachep.movie_project_be.services.impl.AIMovieAdvisorService;
import com.example.thangcachep.movie_project_be.services.impl.MovieAdvisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final MovieAdvisorService movieAdvisorService; // Fallback

    @Autowired(required = false)
    private AIMovieAdvisorService aiMovieAdvisorService; // Optional - chỉ có khi ChatModel được cấu hình

    @PostMapping
    public String handleChat(@RequestBody ChatRequest chatRequest) {
        // Ưu tiên dùng AI service nếu có, ngược lại dùng rule-based
        if (aiMovieAdvisorService != null) {
            try {
                return aiMovieAdvisorService.chat(chatRequest);
            } catch (Exception e) {
                // Fallback về rule-based nếu AI service lỗi
                return movieAdvisorService.chat(chatRequest);
            }
        } else {
            return movieAdvisorService.chat(chatRequest);
        }
    }
}