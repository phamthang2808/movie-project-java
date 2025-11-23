package com.example.thangcachep.movie_project_be.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.thangcachep.movie_project_be.entities.CategoryEntity;
import com.example.thangcachep.movie_project_be.models.request.CategoryRequest;
import com.example.thangcachep.movie_project_be.models.responses.CategoryResponse;
import com.example.thangcachep.movie_project_be.repositories.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Lấy tất cả categories đang active
     * Cache: 6 giờ (categories ít thay đổi)
     * Tạm thời tắt cache để debug: comment @Cacheable
     */
    // @Cacheable(value = "categories", key = "'all'") // Tạm thời tắt cache
    public List<CategoryResponse> getAllCategories() {
        // Lấy tất cả categories và filter active ở service layer
        // (Cách này đảm bảo tương thích với cache và hoạt động như trước)
        List<CategoryEntity> categories = categoryRepository.findAll();
        return categories.stream()
                .filter(cat -> cat.getIsActive() != null && cat.getIsActive()) // Chỉ lấy active
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName())) // Sắp xếp theo tên
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại với ID: " + id));
        return mapToCategoryResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        // Kiểm tra tên category đã tồn tại chưa
        if (categoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Thể loại với tên '" + request.getName() + "' đã tồn tại");
        }

        // Tạo category mới
        // Lưu ý: createdAt và updatedAt sẽ được tự động set bởi @PrePersist trong BaseEntity
        // khi gọi repository.save() lần đầu tiên (khi entity chưa có id)
        CategoryEntity category = CategoryEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        // Save entity - JPA sẽ tự động gọi @PrePersist để set createdAt và updatedAt
        CategoryEntity savedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(savedCategory);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại với ID: " + id));

        // Kiểm tra tên category đã tồn tại chưa (nếu đổi tên)
        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new RuntimeException("Thể loại với tên '" + request.getName() + "' đã tồn tại");
            }
            category.setName(request.getName());
        }

        // Cập nhật các trường khác
        // Lưu ý: Chỉ update các trường được gửi lên (không null)
        // Nếu không gửi trường nào, giá trị cũ sẽ được giữ nguyên

        if (request.getDescription() != null) {
            // Cho phép set description rỗng (để xóa description)
            String desc = request.getDescription().trim();
            category.setDescription(desc.isEmpty() ? null : desc);
        }

        if (request.getImageUrl() != null) {
            // Cho phép set imageUrl rỗng (để xóa URL)
            // Nếu gửi empty string -> set null (xóa URL)
            // Nếu gửi URL -> cập nhật URL mới
            // Nếu không gửi (null) -> giữ nguyên URL cũ
            String imageUrl = request.getImageUrl().trim();
            category.setImageUrl(imageUrl.isEmpty() ? null : imageUrl);
        }
        // Nếu request.getImageUrl() == null -> không update, giữ nguyên giá trị cũ
        // Điều này cho phép bạn chỉ sửa các trường khác mà không thay đổi URL

        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        // Save entity - JPA sẽ tự động gọi @PreUpdate để set updatedAt
        CategoryEntity updatedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(updatedCategory);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại với ID: " + id));

        // Xóa category (cascade sẽ tự động xóa trong bảng movie_categories)
        categoryRepository.delete(category);
    }

    private CategoryResponse mapToCategoryResponse(CategoryEntity category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}


