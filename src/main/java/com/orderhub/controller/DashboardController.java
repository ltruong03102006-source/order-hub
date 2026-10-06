package com.orderhub.controller;

import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.CategoryRevenueResponse;
import com.orderhub.dto.response.CategoryStockResponse;
import com.orderhub.dto.response.DashboardStatsResponse;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.OrderRepository;
import com.orderhub.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "5. Thống Kê & Báo Cáo (Dashboard)", description = "Báo cáo tổng hợp số liệu doanh thu và kho theo danh mục")
public class DashboardController {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @GetMapping("/stats")
    @Operation(summary = "Thống kê tổng quan KPI", description = "Lấy tổng doanh thu, tổng số đơn, tổng sản phẩm và cảnh báo kho")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getOverviewStats() {
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(orderRepository.count())
                .totalProducts(productRepository.count())
                .lowStockProductsCount(0L)
                .build();

        return ResponseEntity.ok(ApiResponse.<DashboardStatsResponse>builder()
                .message("Lấy thống kê tổng quan thành công")
                .data(stats)
                .build());
    }

    @GetMapping("/revenue-by-category")
    @Operation(summary = "Thống kê doanh thu theo danh mục",
            description = "Tính trên các đơn hàng đã xuất kho hoặc giao thành công (SHIPPED/DELIVERED)")
    public ResponseEntity<ApiResponse<List<CategoryRevenueResponse>>> getRevenueByCategory() {
        return ResponseEntity.ok(ApiResponse.<List<CategoryRevenueResponse>>builder()
                .message("Lấy báo cáo doanh thu thành công")
                .data(orderRepository.getRevenueByCategory())
                .build());
    }

    @GetMapping("/stock-by-category")
    @Operation(summary = "Thống kê tồn kho theo danh mục",
            description = "Tổng hợp tồn kho và lượng giữ chỗ theo từng danh mục")
    public ResponseEntity<ApiResponse<List<CategoryStockResponse>>> getStockByCategory() {
        return ResponseEntity.ok(ApiResponse.<List<CategoryStockResponse>>builder()
                .message("Lấy báo cáo tồn kho thành công")
                .data(inventoryRepository.getStockReportByCategory())
                .build());
    }
}