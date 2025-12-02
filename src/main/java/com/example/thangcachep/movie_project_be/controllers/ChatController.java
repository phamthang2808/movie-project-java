package com.example.thangcachep.movie_project_be.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.models.request.ChatRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.ChatResponse;
import com.example.thangcachep.movie_project_be.services.impl.AIMovieAdvisorService;
import com.example.thangcachep.movie_project_be.services.impl.MovieAdvisorService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("${api.prefix}/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final MovieAdvisorService movieAdvisorService; // Fallback
    private final LocalizationUtils localizationUtils;

    @Autowired(required = false)
    private AIMovieAdvisorService aiMovieAdvisorService; // Optional - chỉ có khi ChatModel được cấu hình

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> handleChat(@RequestBody ChatRequest chatRequest) {
        String responseText;
        // Ưu tiên dùng AI service nếu có, ngược lại dùng rule-based
        if (aiMovieAdvisorService != null) {
            try {
                responseText = aiMovieAdvisorService.chat(chatRequest);
            } catch (Exception e) {
                // Fallback về rule-based nếu AI service lỗi
                responseText = movieAdvisorService.chat(chatRequest);
            }
        } else {
            responseText = movieAdvisorService.chat(chatRequest);
        }
        
        ChatResponse chatData = new ChatResponse(responseText);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.CHAT_SEND_SUCCESS);
        ApiResponse<ChatResponse> response = ApiResponse.success(message, chatData);
        return ResponseEntity.ok(response);
    }
}