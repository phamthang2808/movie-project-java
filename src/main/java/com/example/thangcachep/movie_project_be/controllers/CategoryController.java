package com.example.thangcachep.movie_project_be.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.thangcachep.movie_project_be.components.LocalizationUtils;
import com.example.thangcachep.movie_project_be.models.request.CategoryRequest;
import com.example.thangcachep.movie_project_be.models.responses.ApiResponse;
import com.example.thangcachep.movie_project_be.models.responses.CategoryResponse;
import com.example.thangcachep.movie_project_be.services.impl.CategoryService;
import com.example.thangcachep.movie_project_be.utils.MessageKeys;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;
    private final LocalizationUtils localizationUtils;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        String message = localizationUtils.getLocalizedMessage(MessageKeys.CATEGORY_GET_ALL_SUCCESS);
        ApiResponse<List<CategoryResponse>> response = ApiResponse.success(message, categories);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.CATEGORY_GET_SUCCESS);
        ApiResponse<CategoryResponse> response = ApiResponse.success(message, category);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.CATEGORY_CREATE_SUCCESS);
        ApiResponse<CategoryResponse> response = ApiResponse.success(message, 201, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.CATEGORY_UPDATE_SUCCESS);
        ApiResponse<CategoryResponse> response = ApiResponse.success(message, category);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        String message = localizationUtils.getLocalizedMessage(MessageKeys.CATEGORY_DELETE_SUCCESS);
        ApiResponse<Void> response = ApiResponse.success(message, null);
        return ResponseEntity.ok(response);
    }
}


