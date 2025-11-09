package com.example.thangcachep.movie_project_be.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchHistoryResponse {
    private MovieResponse movie;
    private Integer watchProgress; // Tiến độ xem (giây)
    private Integer totalDuration; // Tổng thời lượng (giây)
    private Long currentEpisodeId; // ID tập phim đang xem (nếu là series)
    private Integer currentEpisodeNumber; // Số tập phim đang xem (nếu là series)
}

