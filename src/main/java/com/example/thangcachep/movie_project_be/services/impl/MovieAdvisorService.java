package com.example.thangcachep.movie_project_be.services.impl;

import com.example.thangcachep.movie_project_be.entities.CategoryEntity;
import com.example.thangcachep.movie_project_be.entities.MovieEntity;
import com.example.thangcachep.movie_project_be.models.request.ChatRequest;
import com.example.thangcachep.movie_project_be.repositories.CategoryRepository;
import com.example.thangcachep.movie_project_be.repositories.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieAdvisorService {

    private final MovieRepository movieRepository;
    private final CategoryRepository categoryRepository;

    public String chat(ChatRequest chatRequest) {
        String message = chatRequest.message().toLowerCase().trim();

        // Xử lý các câu hỏi phổ biến
        if (message.contains("xin chào") || message.contains("hello") || message.contains("hi")) {
            return "Xin chào! Tôi là trợ lý tư vấn phim. Tôi có thể giúp bạn:\n" +
                    "• Tìm phim theo thể loại (ví dụ: 'phim hành động', 'phim tình cảm')\n" +
                    "• Tìm phim hay, phim mới\n" +
                    "• Tìm phim theo từ khóa\n" +
                    "• Tìm phim bộ hoặc phim lẻ\n" +
                    "Bạn muốn tìm phim gì?";
        }

        if (message.contains("giúp") || message.contains("help") || message.contains("hướng dẫn")) {
            return "Tôi có thể giúp bạn tìm phim bằng cách:\n" +
                    "• 'phim về [thể loại]' - Tìm phim theo thể loại\n" +
                    "• 'phim hay' - Phim có rating cao\n" +
                    "• 'phim mới' - Phim mới phát hành\n" +
                    "• 'phim [từ khóa]' - Tìm phim theo tên hoặc mô tả\n" +
                    "• 'phim bộ' - Danh sách phim bộ\n" +
                    "• 'phim lẻ' - Danh sách phim lẻ\n" +
                    "Ví dụ: 'phim hành động hay', 'phim tình cảm mới'";
        }

        // Parse và tìm phim
        List<MovieEntity> movies = findMoviesByQuery(message);

        if (movies.isEmpty()) {
            return "Xin lỗi, tôi không tìm thấy phim nào phù hợp với yêu cầu của bạn. " +
                    "Bạn có thể thử:\n" +
                    "• Tìm theo thể loại (hành động, tình cảm, kinh dị...)\n" +
                    "• Tìm 'phim hay' hoặc 'phim mới'\n" +
                    "• Hoặc mô tả cụ thể hơn về phim bạn muốn xem";
        }

        // Tạo câu trả lời
        return formatMovieRecommendation(movies, message);
    }

    private List<MovieEntity> findMoviesByQuery(String query) {
        List<MovieEntity> results = new ArrayList<>();

        // 1. Tìm phim hay (top rated)
        if (query.contains("hay") || query.contains("đánh giá cao") || query.contains("rating cao")) {
            List<MovieEntity> topRated = movieRepository.findTopRatedMovies(PageRequest.of(0, 10));
            if (!topRated.isEmpty()) {
                return topRated;
            }
        }

        // 2. Tìm phim mới (phim có releaseDate gần đây)
        if (query.contains("mới") || query.contains("mới phát hành") || query.contains("recent")) {
            LocalDate oneYearAgo = LocalDate.now().minusYears(1);
            List<MovieEntity> recent = movieRepository.findUpcomingMovies(
                    oneYearAgo, PageRequest.of(0, 10));
            if (!recent.isEmpty()) {
                return recent;
            }
            // Fallback: lấy phim mới nhất theo createdAt
            List<MovieEntity> allActive = movieRepository.findByIsActiveTrueWithCategories();
            return allActive.stream()
                    .filter(m -> m.getStatus() != MovieEntity.MovieStatus.PENDING &&
                            m.getStatus() != MovieEntity.MovieStatus.REJECTED)
                    .sorted((m1, m2) -> {
                        if (m1.getCreatedAt() != null && m2.getCreatedAt() != null) {
                            return m2.getCreatedAt().compareTo(m1.getCreatedAt());
                        }
                        return 0;
                    })
                    .limit(10)
                    .collect(Collectors.toList());
        }

        // 3. Tìm theo thể loại
        List<CategoryEntity> allCategories = categoryRepository.findAll();
        CategoryEntity matchedCategory = null;
        for (CategoryEntity category : allCategories) {
            String categoryName = category.getName().toLowerCase();
            // Tìm thể loại trong câu hỏi
            if (query.contains(categoryName) ||
                    query.contains("về " + categoryName) ||
                    query.contains("thể loại " + categoryName)) {
                matchedCategory = category;
                break;
            }
        }

        if (matchedCategory != null) {
            List<MovieEntity> byCategory = movieRepository.findByCategoryId(
                    matchedCategory.getId(), PageRequest.of(0, 10)).getContent();
            if (!byCategory.isEmpty()) {
                // Nếu có thêm từ "hay", sắp xếp theo rating
                if (query.contains("hay")) {
                    byCategory.sort((m1, m2) -> {
                        Double r1 = m1.getRating() != null ? m1.getRating() : 0.0;
                        Double r2 = m2.getRating() != null ? m2.getRating() : 0.0;
                        return r2.compareTo(r1);
                    });
                }
                return byCategory;
            }
        }

        // 4. Tìm phim bộ hoặc phim lẻ
        if (query.contains("phim bộ") || query.contains("series")) {
            List<MovieEntity> series = movieRepository.findByTypeAndIsActiveTrue(
                    MovieEntity.MovieType.SERIES, PageRequest.of(0, 10)).getContent();
            if (!series.isEmpty()) {
                return series;
            }
        }

        if (query.contains("phim lẻ") || query.contains("movie") && !query.contains("phim bộ")) {
            List<MovieEntity> movies = movieRepository.findByTypeAndIsActiveTrue(
                    MovieEntity.MovieType.MOVIE, PageRequest.of(0, 10)).getContent();
            if (!movies.isEmpty()) {
                return movies;
            }
        }

        // 5. Tìm theo từ khóa trong title hoặc description
        String keyword = extractKeyword(query);
        if (keyword != null && keyword.length() > 2) {
            List<MovieEntity> searchResults = movieRepository.searchMovies(
                    keyword, PageRequest.of(0, 10)).getContent();
            if (!searchResults.isEmpty()) {
                return searchResults;
            }
        }

        // 6. Tìm phim trending (nhiều lượt xem)
        if (query.contains("trending") || query.contains("phổ biến") || query.contains("nhiều người xem")) {
            List<MovieEntity> trending = movieRepository.findTopViewedMovies(PageRequest.of(0, 10));
            if (!trending.isEmpty()) {
                return trending;
            }
        }

        // 7. Mặc định: trả về top rated nếu không tìm thấy gì
        List<MovieEntity> topRated = movieRepository.findTopRatedMovies(PageRequest.of(0, 5));
        if (!topRated.isEmpty()) {
            return topRated;
        }

        // Fallback cuối cùng: lấy một số phim active bất kỳ
        List<MovieEntity> allActive = movieRepository.findByIsActiveTrueWithCategories();
        return allActive.stream()
                .filter(m -> m.getStatus() != MovieEntity.MovieStatus.PENDING &&
                        m.getStatus() != MovieEntity.MovieStatus.REJECTED)
                .limit(5)
                .collect(Collectors.toList());
    }

    private String extractKeyword(String query) {
        // Loại bỏ các từ không cần thiết
        String[] stopWords = {"phim", "về", "có", "nào", "hay", "mới", "bộ", "lẻ", "theo", "tìm", "cho", "tôi", "bạn", "của", "và", "hoặc"};

        // Loại bỏ các pattern đã xử lý
        query = query.replaceAll("phim (bộ|lẻ|hay|mới)", "");
        query = query.replaceAll("(về|theo|tìm|cho)", "");

        String[] words = query.trim().split("\\s+");

        // Tìm từ khóa dài nhất (có thể là cụm từ)
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

    private String formatMovieRecommendation(List<MovieEntity> movies, String query) {
        if (movies.isEmpty()) {
            return "Không tìm thấy phim nào phù hợp.";
        }

        StringBuilder response = new StringBuilder();

        // Phần giới thiệu
        if (query.contains("hay")) {
            response.append("Đây là những phim hay nhất trong hệ thống:\n\n");
        } else if (query.contains("mới")) {
            response.append("Đây là những phim mới phát hành:\n\n");
        } else {
            response.append("Tôi tìm thấy ").append(movies.size()).append(" phim phù hợp:\n\n");
        }

        // Liệt kê phim
        int count = Math.min(movies.size(), 10);
        for (int i = 0; i < count; i++) {
            MovieEntity movie = movies.get(i);
            response.append((i + 1)).append(". **").append(movie.getTitle()).append("**");

            if (movie.getRating() != null && movie.getRating() > 0) {
                response.append(" ⭐ ").append(String.format("%.1f", movie.getRating()));
            }

            if (movie.getReleaseDate() != null) {
                response.append(" (").append(movie.getReleaseDate().getYear()).append(")");
            }

            if (movie.getDuration() != null) {
                int hours = movie.getDuration() / 60;
                int minutes = movie.getDuration() % 60;
                if (hours > 0) {
                    response.append(" - ").append(hours).append("h");
                    if (minutes > 0) {
                        response.append(" ").append(minutes).append("m");
                    }
                } else {
                    response.append(" - ").append(minutes).append(" phút");
                }
            }

            // Thêm type
            if (movie.getType() != null) {
                response.append(" - ").append(movie.getType() == MovieEntity.MovieType.SERIES ? "Phim bộ" : "Phim lẻ");
            }

            response.append("\n");

            // Thêm mô tả ngắn nếu có
            if (movie.getDescription() != null && movie.getDescription().length() > 0) {
                String desc = movie.getDescription().trim();
                if (desc.length() > 120) {
                    desc = desc.substring(0, 120) + "...";
                }
                response.append("   ").append(desc).append("\n");
            }

            response.append("\n");
        }

        if (count < movies.size()) {
            response.append("... và còn ").append(movies.size() - count).append(" phim khác.\n\n");
        }

        response.append("Bạn có thể xem chi tiết và xem phim tại trang chi tiết của từng phim.");

        return response.toString();
    }
}

