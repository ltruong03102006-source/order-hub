package com.orderhub.service.impl;

import com.orderhub.dto.response.DashboardStatsResponse;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.OrderRepository;
import com.orderhub.repository.ProductRepository;
import com.orderhub.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.count();
        // Coi như hàng tồn < 10 là sắp hết hàng
        long lowStockCount = inventoryRepository.countLowStockProducts(10);

        return DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .lowStockProductsCount(lowStockCount)
                .build();
    }
}