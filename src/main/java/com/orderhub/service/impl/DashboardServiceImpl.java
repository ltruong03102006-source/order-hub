package com.orderhub.service.impl;

import com.orderhub.dto.response.DashboardStatsResponse;
import com.orderhub.entity.enums.OrderStatus;
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

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        BigDecimal totalCost = orderRepository.calculateTotalCostOfGoodsSold();
        boolean costTrackingComplete = orderRepository.countUncostedShippedItems() == 0;
        long totalOrders = orderRepository.count();
        long totalProducts = productRepository.count();
        long lowStockCount = inventoryRepository.countLowStockProducts(LOW_STOCK_THRESHOLD);

        return DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalCostOfGoodsSold(totalCost)
                .grossProfit(costTrackingComplete ? totalRevenue.subtract(totalCost) : null)
                .costTrackingComplete(costTrackingComplete)
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .lowStockProductsCount(lowStockCount)
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                .confirmedOrders(orderRepository.countByStatus(OrderStatus.CONFIRMED))
                .shippedOrders(orderRepository.countByStatus(OrderStatus.SHIPPED))
                .lowStockThreshold(LOW_STOCK_THRESHOLD)
                .build();
    }
}