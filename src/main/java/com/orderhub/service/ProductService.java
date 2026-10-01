package com.orderhub.service;

import com.orderhub.dto.request.CreateProductRequest;
import com.orderhub.dto.request.UpdateStockRequest;
import com.orderhub.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(CreateProductRequest request);
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Long id);
    ProductResponse updateStock(Long id, UpdateStockRequest request);
}