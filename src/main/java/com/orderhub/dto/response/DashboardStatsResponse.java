package com.orderhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private BigDecimal totalRevenue;      // Tổng doanh thu
    private BigDecimal totalCostOfGoodsSold;
    private BigDecimal grossProfit;
    private boolean costTrackingComplete;
    private long totalOrders;              // Tổng số đơn hàng
    private long totalProducts;            // Tổng số sản phẩm đang quản lý
    private long lowStockProductsCount;    // Số sản phẩm có tồn kho thấp (ví dụ < 10)
    private long pendingOrders;
    private long confirmedOrders;
    private long shippedOrders;
    private int lowStockThreshold;
}