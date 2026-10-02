package com.orderhub.service.impl;

import com.orderhub.dto.request.CreateOrderRequest;
import com.orderhub.dto.request.OrderItemRequest;
import com.orderhub.dto.request.UpdateOrderStatusRequest;
import com.orderhub.dto.response.OrderItemResponse;
import com.orderhub.dto.response.OrderResponse;
import com.orderhub.entity.Order;
import com.orderhub.entity.OrderItem;
import com.orderhub.entity.Product;
import com.orderhub.entity.User;
import com.orderhub.entity.enums.OrderStatus;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.OrderRepository;
import com.orderhub.repository.ProductRepository;
import com.orderhub.repository.UserRepository;
import com.orderhub.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1. Kiểm tra User tồn tại
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Khởi tạo thực thể Order
        String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Xử lý từng item trong đơn hàng
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            // Trừ kho an toàn qua InventoryRepository (Tăng reservedQuantity)
            int updatedRows = inventoryRepository.reserveStock(product.getId(), itemRequest.getQuantity());
            if (updatedRows == 0) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            BigDecimal itemPrice = product.getPrice();
            BigDecimal subtotal = itemPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .priceAtPurchase(itemPrice)
                    .build();

            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);

        // 4. Lưu đơn hàng (CascadeType.ALL tự động lưu kèm danh sách order_items)
        Order savedOrder = orderRepository.save(order);

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        // Kiểm tra user có tồn tại
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return orderRepository.findAll().stream()
                .filter(order -> order.getUser().getId().equals(userId))
                .map(this::mapToOrderResponse)
                .toList();
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrderStatus(String orderCode, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.getStatus();

        // 1. Kiểm tra tính hợp lệ của việc chuyển trạng thái
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        // 2. Kích hoạt nghiệp vụ kho tương ứng
        if (targetStatus == OrderStatus.CANCELLED) {
            // Hoàn lại lượng reservedQuantity về cho kho
            for (OrderItem item : order.getItems()) {
                inventoryRepository.releaseStock(item.getProduct().getId(), item.getQuantity());
            }
        } else if (targetStatus == OrderStatus.SHIPPED) {
            // Hàng xuất đi: trừ đứt cả totalQuantity lẫn reservedQuantity
            for (OrderItem item : order.getItems()) {
                inventoryRepository.deductStockOnShipment(item.getProduct().getId(), item.getQuantity());
            }
        }

        // 3. Cập nhật trạng thái mới cho đơn hàng
        order.setStatus(targetStatus);
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderResponse(updatedOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse cancelOrder(String orderCode) {
        // Hủy đơn hàng là trường hợp đặc biệt chuyển sang CANCELLED
        return updateOrderStatus(orderCode, UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build());
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .sku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .subtotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser().getId())
                .username(order.getUser().getUsername())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }
}