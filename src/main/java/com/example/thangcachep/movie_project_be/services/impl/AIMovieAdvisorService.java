package com.example.thangcachep.movie_project_be.services.impl;

import com.example.thangcachep.movie_project_be.entities.CategoryEntity;
import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.models.request.ChatRequest;
import com.example.thangcachep.movie_project_be.repositories.CategoryRepository;
import com.example.thangcachep.movie_project_be.repositories.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(ChatModel.class)
public class AIMovieAdvisorService {

    private final ChatModel chatModel;
    private final MovieRepository movieRepository;
    private final CategoryRepository categoryRepository;

    public String chat(ChatRequest chatRequest) {
        String userMessage = chatRequest.message().trim();

        // Tìm phim liên quan bằng keyword search
        List<MovieEntity> relevantMovies = findRelevantMovies(userMessage);

        // Tạo context từ dữ liệu phim
        String context = buildMovieContext(relevantMovies, userMessage);

        // Tạo system prompt với context
        String systemPrompt = buildSystemPrompt(context);

        // Gọi AI với context
        Prompt prompt = new Prompt(
                Arrays.asList(
                        new SystemMessage(systemPrompt),
                        new UserMessage(userMessage)
                )
        );

        return chatModel.call(prompt).getResult().getOutput().getContent();
    }

    private List<MovieEntity> findRelevantMovies(String query) {
        String lowerQuery = query.toLowerCase();
        List<MovieEntity> results = new ArrayList<>();
        Set<Long> addedIds = new HashSet<>();

        // 1. Tìm phim hay (top rated)
        if (lowerQuery.contains("hay") || lowerQuery.contains("đánh giá cao") || lowerQuery.contains("rating cao")) {
            List<MovieEntity> topRated = movieRepository.findTopRatedMovies(PageRequest.of(0, 5));
            topRated.forEach(m -> {
                if (!addedIds.contains(m.getId())) {
                    results.add(m);
                    addedIds.add(m.getId());
                }
            });
        }

        // 2. Tìm phim mới
        if (lowerQuery.contains("mới") || lowerQuery.contains("mới phát hành")) {
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);
            List<MovieEntity> recent = movieRepository.findUpcomingMovies(oneYearAgo, PageRequest.of(0, 5));
            recent.forEach(m -> {
                if (!addedIds.contains(m.getId())) {
                    results.add(m);
                    addedIds.add(m.getId());
                }
            });
        }

        // 3. Tìm theo thể loại
        List<CategoryEntity> allCategories = categoryRepository.findAll();
        for (CategoryEntity category : allCategories) {
            String categoryName = category.getName().toLowerCase();
            if (lowerQuery.contains(categoryName) || lowerQuery.contains("về " + categoryName)) {
                List<MovieEntity> byCategory = movieRepository.findByCategoryId(
                        category.getId(), PageRequest.of(0, 5)).getContent();
                byCategory.forEach(m -> {
                    if (!addedIds.contains(m.getId())) {
                        results.add(m);
                        addedIds.add(m.getId());
                    }
                });
                break;
            }
        }

        // 4. Tìm phim bộ/lẻ
        if (lowerQuery.contains("phim bộ") || lowerQuery.contains("series")) {
            List<MovieEntity> series = movieRepository.findByTypeAndIsActiveTrue(
                    MovieEntity.MovieType.SERIES, PageRequest.of(0, 5)).getContent();
            series.forEach(m -> {
                if (!addedIds.contains(m.getId())) {
                    results.add(m);
                    addedIds.add(m.getId());
                }
            });
        }

        if (lowerQuery.contains("phim lẻ") || (lowerQuery.contains("movie") && !lowerQuery.contains("phim bộ"))) {
            List<MovieEntity> movies = movieRepository.findByTypeAndIsActiveTrue(
                    MovieEntity.MovieType.MOVIE, PageRequest.of(0, 5)).getContent();
            movies.forEach(m -> {
                if (!addedIds.contains(m.getId())) {
                    results.add(m);
                    addedIds.add(m.getId());
                }
            });
        }

        // 5. Tìm theo từ khóa
        String keyword = extractKeyword(lowerQuery);
        if (keyword != null && keyword.length() > 2) {
            List<MovieEntity> searchResults = movieRepository.searchMovies(
                    keyword, PageRequest.of(0, 5)).getContent();
            searchResults.forEach(m -> {
                if (!addedIds.contains(m.getId())) {
                    results.add(m);
                    addedIds.add(m.getId());
                }
            });
        }

        // 6. Fallback: top rated
        if (results.isEmpty()) {
            List<MovieEntity> topRated = movieRepository.findTopRatedMovies(PageRequest.of(0, 5));
            results.addAll(topRated);
        }

        return results.stream().limit(10).collect(Collectors.toList());
    }

    private String extractKeyword(String query) {
        String[] stopWords = {"phim", "về", "có", "nào", "hay", "mới", "bộ", "lẻ", "theo", "tìm", "cho", "tôi", "bạn"};
        query = query.replaceAll("phim (bộ|lẻ|hay|mới)", "");
        query = query.replaceAll("(về|theo|tìm|cho)", "");

        String[] words = query.trim().split("\\s+");
        StringBuilder keyword = new StringBuilder();
        for (String word : words) {
            word = word.trim();
            if (word.length() > 2 && !Arrays.asList(stopWords).contains(word.toLowerCase())) {
                if (keyword.length() > 0) {
                    keyword.append(" ");
                }
                keyword.append(word);
            }
        }

        String result = keyword.toString().trim();
        return result.length() > 2 ? result : null;
    }

    private String buildMovieContext(List<MovieEntity> movies, String userQuery) {
        if (movies.isEmpty()) {
            return "Không có phim nào trong hệ thống.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Dữ liệu phim trong hệ thống:\n\n");

        for (int i = 0; i < movies.size(); i++) {
            MovieEntity movie = movies.get(i);
            context.append((i + 1)).append(". ").append(movie.getTitle());

            if (movie.getRating() != null && movie.getRating() > 0) {
                context.append(" (Đánh giá: ").append(String.format("%.1f", movie.getRating())).append("/10)");
            }

            if (movie.getReleaseDate() != null) {
                context.append(" - Năm: ").append(movie.getReleaseDate().getYear());
            }

            if (movie.getDuration() != null) {
                int hours = movie.getDuration() / 60;
                int minutes = movie.getDuration() % 60;
                if (hours > 0) {
                    context.append(" - Thời lượng: ").append(hours).append("h");
                    if (minutes > 0) {
                        context.append(" ").append(minutes).append("m");
                    }
                } else {
                    context.append(" - Thời lượng: ").append(minutes).append(" phút");
                }
            }

            if (movie.getType() != null) {
                context.append(" - Loại: ").append(
                        movie.getType() == MovieEntity.MovieType.SERIES ? "Phim bộ" : "Phim lẻ"
                );
            }

            if (movie.getDescription() != null && !movie.getDescription().trim().isEmpty()) {
                String desc = movie.getDescription().trim();
                if (desc.length() > 150) {
                    desc = desc.substring(0, 150) + "...";
                }
                context.append("\n   Mô tả: ").append(desc);
            }

            // Thêm thể loại
            if (movie.getCategories() != null && !movie.getCategories().isEmpty()) {
                String categories = movie.getCategories().stream()
                        .map(CategoryEntity::getName)
                        .collect(Collectors.joining(", "));
                context.append("\n   Thể loại: ").append(categories);
            }

            context.append("\n\n");
        }

        return context.toString();
    }

    private String buildSystemPrompt(String movieContext) {
        return "Bạn là trợ lý tư vấn phim thông minh và thân thiện. " +
                "Nhiệm vụ của bạn là giúp người dùng tìm phim phù hợp dựa trên dữ liệu phim trong hệ thống.\n\n" +
                "Dữ liệu phim hiện có:\n" + movieContext + "\n" +
                "Hướng dẫn:\n" +
                "1. Trả lời bằng tiếng Việt, tự nhiên và thân thiện\n" +
                "2. Chỉ đề xuất phim có trong dữ liệu trên\n" +
                "3. Nếu người dùng hỏi về phim không có trong dữ liệu, hãy đề xuất phim tương tự\n" +
                "4. Đưa ra lý do tại sao phim đó phù hợp với yêu cầu\n" +
                "5. Có thể so sánh các phim nếu có nhiều lựa chọn\n" +
                "6. Nếu không tìm thấy phim phù hợp, hãy đề xuất phim hay nhất trong hệ thống\n" +
                "7. Luôn kết thúc bằng cách mời người dùng xem chi tiết phim\n" +
                "8. Không bịa đặt thông tin không có trong dữ liệu\n" +
                "9. Sử dụng emoji một cách hợp lý để làm cho câu trả lời sinh động hơn";
    }
}

