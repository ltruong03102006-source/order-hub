package com.orderhub.service;

import com.orderhub.dto.request.CreateCategoryRequest;
import com.orderhub.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);
    List<CategoryResponse> getAllCategories();
}