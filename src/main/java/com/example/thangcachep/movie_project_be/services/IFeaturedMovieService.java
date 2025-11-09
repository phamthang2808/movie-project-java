package com.example.thangcachep.movie_project_be.services;


import com.example.thangcachep.movie_project_be.models.dto.FeaturedMovieDTO;

import java.util.List;

/**
 * Service cho Featured Movies
 * Chỉ có method để lấy danh sách 6 phim nổi bật
 */
public interface IFeaturedMovieService {

    /**
     * Lấy danh sách 6 phim nổi bật đang active
     */
    List<FeaturedMovieDTO> getFeaturedMovies();
}
