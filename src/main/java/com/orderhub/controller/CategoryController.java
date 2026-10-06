package com.orderhub.controller;

import com.orderhub.dto.request.CreateCategoryRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.CategoryResponse;
import com.orderhub.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "4. Quản Lý Danh Mục (Categories)", description = "Cung cấp danh mục cho dropdown khi tạo sản phẩm và bộ lọc tìm kiếm")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Tạo danh mục mới")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .message("Tạo danh mục thành công")
                .data(categoryService.createCategory(request))
                .build());
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả danh mục (Dùng đổ vào Dropdown)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .message("Lấy danh sách danh mục thành công")
                .data(categoryService.getAllCategories())
                .build());
    }

    // 1. API SỬA DANH MỤC
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật danh mục theo ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryRequest request) { // Dùng DTO request tạo danh mục của bạn
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .message("Cập nhật danh mục thành công")
                        .data(categoryService.updateCategory(id, request))
                        .build()
        );
    }

    // 2. API XÓA DANH MỤC
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa danh mục theo ID")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Xóa danh mục thành công")
                        .build()
        );
    }
}