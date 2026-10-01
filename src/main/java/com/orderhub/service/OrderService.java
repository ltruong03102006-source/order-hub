package com.orderhub.service;

import com.orderhub.dto.request.CreateOrderRequest;
import com.orderhub.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderByCode(String orderCode);
    List<OrderResponse> getOrdersByUserId(Long userId);
}