package com.orderhub.controller;

import com.orderhub.dto.request.CreateProductRequest;
import com.orderhub.dto.request.UpdateProductRequest;
import com.orderhub.dto.request.UpdateStockRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.ProductResponse;
import com.orderhub.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Controller", description = "Quản lý sản phẩm và tồn kho")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Tạo sản phẩm mới")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ProductResponse>builder()
                        .code(HttpStatus.CREATED.value())
                        .message("Tạo sản phẩm thành công")
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả sản phẩm")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(
                ApiResponse.<List<ProductResponse>>builder()
                        .data(productService.getAllProducts())
                        .build()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sản phẩm theo ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .data(productService.getProductById(id))
                        .build()
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin sản phẩm (không ảnh hưởng tồn kho)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) { // <-- Dùng UpdateProductRequest
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .message("Cập nhật thông tin sản phẩm thành công")
                        .data(productService.updateProduct(id, request))
                        .build()
        );
    }

    // 2. THÊM API XÓA SẢN PHẨM
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa sản phẩm theo ID")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Xóa sản phẩm thành công")
                        .build()
        );
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Cập nhật số lượng tồn kho")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .message("Cập nhật kho thành công")
                        .data(productService.updateStock(id, request, authentication.getName()))
                        .build()
        );
    }
}