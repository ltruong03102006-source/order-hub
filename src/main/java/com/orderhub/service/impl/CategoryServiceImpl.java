package com.orderhub.service.impl;

import com.orderhub.dto.request.CreateCategoryRequest;
import com.orderhub.dto.response.CategoryResponse;
import com.orderhub.entity.Category;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.CategoryRepository;
import com.orderhub.repository.ProductRepository;
import com.orderhub.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository; // Dùng để kiểm tra ràng buộc trước khi xóa

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName()) || categoryRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Tên hoặc mã danh mục đã tồn tại trong hệ thống");
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .code(request.getCode().trim().toUpperCase())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    // 1. THÊM HÀM SỬA DANH MỤC
    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        // Kiểm tra nếu đổi tên trùng với danh mục khác
        if (!category.getName().equalsIgnoreCase(request.getName().trim())
                && categoryRepository.existsByName(request.getName().trim())) {
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }

        category.setName(request.getName().trim());
        if (request.getCode() != null && !request.getCode().isBlank()) {
            category.setCode(request.getCode().trim().toUpperCase());
        }
        category.setDescription(request.getDescription());

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    // 2. THÊM HÀM XÓA DANH MỤC
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        // Kiểm tra an toàn: Không cho xóa nếu danh mục đang chứa sản phẩm
        boolean hasProducts = productRepository.findAll().stream()
                .anyMatch(p -> p.getCategory() != null && p.getCategory().getId().equals(id));
        if (hasProducts) {
            throw new RuntimeException("Không thể xóa danh mục này vì đang có sản phẩm thuộc danh mục!");
        }

        categoryRepository.delete(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .code(category.getCode())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}