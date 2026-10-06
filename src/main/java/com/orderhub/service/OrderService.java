package com.orderhub.service;

import com.orderhub.dto.request.CreateOrderRequest;
import com.orderhub.dto.request.UpdateOrderStatusRequest;
import com.orderhub.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderByCode(String orderCode);
    List<OrderResponse> getOrdersByUserId(Long userId);
    OrderResponse updateOrderStatus(String orderCode, UpdateOrderStatusRequest request);
    OrderResponse cancelOrder(String orderCode);
    List<OrderResponse> getAllOrders();
}