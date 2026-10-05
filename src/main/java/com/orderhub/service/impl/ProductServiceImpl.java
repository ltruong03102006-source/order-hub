package com.orderhub.service.impl;

import com.orderhub.dto.request.CreateProductRequest;
import com.orderhub.dto.request.UpdateStockRequest;
import com.orderhub.dto.response.ProductResponse;
import com.orderhub.entity.Category;
import com.orderhub.entity.Inventory;
import com.orderhub.entity.Product;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.CategoryRepository;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.ProductRepository;
import com.orderhub.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.findBySku(request.getSku()).isPresent()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT));
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .price(request.getPrice())
                .build();

        Inventory inventory = Inventory.builder()
                .product(product)
                .totalQuantity(request.getStockQuantity())
                .reservedQuantity(0)
                .build();

        product.setInventory(inventory);

        return mapToResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long id, UpdateStockRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        inventory.setTotalQuantity(request.getQuantity());
        inventoryRepository.save(inventory);

        return mapToResponse(inventory.getProduct());
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}