package com.orderhub.service;

import com.orderhub.dto.request.CreateOrderRequest;
import com.orderhub.dto.request.UpdateOrderStatusRequest;
import com.orderhub.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, String createdByUsername);
    OrderResponse getOrderByCode(String orderCode);
    List<OrderResponse> getOrdersByUserId(Long userId);
    OrderResponse updateOrderStatus(String orderCode, UpdateOrderStatusRequest request, String performedBy);
    OrderResponse cancelOrder(String orderCode, String performedBy);
    List<OrderResponse> getAllOrders();
}