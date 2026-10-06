package com.orderhub.service.impl;

import com.orderhub.dto.request.CreateProductRequest;
import com.orderhub.dto.request.UpdateProductRequest;
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
                .category(category) // 1. Bổ sung gán category vào product ở đây
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

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + id));

        if (request.getSku() != null && !request.getSku().isBlank()) {
            product.setSku(request.getSku());
        }
        product.setName(request.getName());
        product.setPrice(request.getPrice());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        Product updatedProduct = productRepository.save(product);

        // Dùng hàm map response sẵn có của bạn hoặc builder:
        return ProductResponse.builder()
                .id(updatedProduct.getId())
                .sku(updatedProduct.getSku())
                .name(updatedProduct.getName())
                .price(updatedProduct.getPrice())
                .categoryName(updatedProduct.getCategory() != null ? updatedProduct.getCategory().getName() : null)
                .build();
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy sản phẩm với id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product product) {
        Inventory inv = product.getInventory();

        int available = (inv != null && inv.getAvailableQuantity() != null) ? inv.getAvailableQuantity() : 0;
        int reserved = (inv != null && inv.getReservedQuantity() != null) ? inv.getReservedQuantity() : 0;
        int total = (inv != null && inv.getTotalQuantity() != null) ? inv.getTotalQuantity() : (available + reserved);

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .price(product.getPrice())
                .stockQuantity(available)
                .totalQuantity(total)
                .reservedQuantity(product.getInventory() != null ? product.getInventory().getReservedQuantity() : 0)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

}