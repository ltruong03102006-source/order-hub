package com.orderhub.service.impl;

import com.orderhub.dto.request.CreateOrderRequest;
import com.orderhub.dto.request.OrderItemRequest;
import com.orderhub.dto.request.UpdateOrderStatusRequest;
import com.orderhub.dto.response.OrderItemResponse;
import com.orderhub.dto.response.OrderResponse;
import com.orderhub.entity.*;
import com.orderhub.entity.enums.InventoryActionType;
import com.orderhub.entity.enums.OrderStatus;
import com.orderhub.event.OrderCreatedEvent;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.*;
import com.orderhub.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final InventoryBatchRepository inventoryBatchRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventoryLogRepository inventoryLogRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request, String createdByUsername) {
        User user = userRepository.findByUsername(createdByUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Khởi tạo thực thể Order
        String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .receiverName(normalize(request.getReceiverName()))
                .receiverPhone(normalize(request.getReceiverPhone()))
                .shippingAddress(normalize(request.getShippingAddress()))
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
            inventoryLogRepository.save(InventoryLog.builder()
                    .productId(product.getId())
                    .changeAmount(itemRequest.getQuantity())
                    .actionType(InventoryActionType.RESERVE)
                    .referenceOrderCode(orderCode) // Biến orderCode bạn đã sinh trước đó
                    .note("Giữ chỗ tồn kho khi tạo đơn hàng mới")
                    .performedBy(createdByUsername)
                    .build());

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

        // Bắn sự kiện OrderCreatedEvent
        eventPublisher.publishEvent(new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getOrderCode(),
                savedOrder.getUser().getId(),
                savedOrder.getTotalAmount(),
                savedOrder.getCreatedAt()
        ));

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
    public OrderResponse updateOrderStatus(
            String orderCode, UpdateOrderStatusRequest request, String performedBy) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus targetStatus = request.getStatus();

        // 1. Kiểm tra tính hợp lệ của việc chuyển trạng thái theo State Machine
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new AppException(ErrorCode.INVALID_STATE_TRANSITION);
        }

        // 2. Kích hoạt nghiệp vụ kho tương ứng
        if (targetStatus == OrderStatus.CANCELLED) {
            // Hoàn lại lượng reservedQuantity về cho kho (khi đơn chưa xuất đi)
            for (OrderItem item : order.getItems()) {
                inventoryRepository.releaseStock(item.getProduct().getId(), item.getQuantity());

                inventoryLogRepository.save(InventoryLog.builder()
                        .productId(item.getProduct().getId())
                        .changeAmount(-item.getQuantity())
                        .actionType(InventoryActionType.RELEASE)
                        .referenceOrderCode(order.getOrderCode())
                        .note("Hoàn trả lượng giữ chỗ do đơn hàng bị hủy")
                        .performedBy(performedBy)
                        .build());
            }
        } else if (targetStatus == OrderStatus.SHIPPED) {
            for (OrderItem item : order.getItems()) {
                allocateFifoCost(item);
                int updatedRows = inventoryRepository.deductStockOnShipment(
                        item.getProduct().getId(), item.getQuantity());
                if (updatedRows != 1) {
                    throw new AppException(ErrorCode.INVENTORY_BATCH_MISMATCH);
                }

                inventoryLogRepository.save(InventoryLog.builder()
                        .productId(item.getProduct().getId())
                        .changeAmount(-item.getQuantity())
                        .actionType(InventoryActionType.SHIP_DEDUCT)
                        .referenceOrderCode(order.getOrderCode())
                        .note("Xuất kho bàn giao cho đơn vị vận chuyển")
                        .performedBy(performedBy)
                        .build());
            }
        } else if (targetStatus == OrderStatus.RETURNED) {
            for (OrderItem item : order.getItems()) {
                restoreFifoBatches(item);
                int updatedRows = inventoryRepository.restockOnReturn(
                        item.getProduct().getId(), item.getQuantity());
                if (updatedRows != 1) {
                    throw new AppException(ErrorCode.INVENTORY_BATCH_MISMATCH);
                }

                inventoryLogRepository.save(InventoryLog.builder()
                        .productId(item.getProduct().getId())
                        .changeAmount(item.getQuantity()) // Số dương vì kho được cộng lại hàng
                        .actionType(InventoryActionType.RETURN_RESTOCK)
                        .referenceOrderCode(order.getOrderCode())
                        .note("Nhập lại kho do giao hàng không thành công / khách hoàn đơn")
                        .performedBy(performedBy)
                        .build());
            }
        } else if (targetStatus == OrderStatus.DELIVERED) {
            // Giao thành công: Không cần can thiệp kho, đơn đóng thành công
            // Có thể ghi log thông báo nếu cần thiết
        }

        // 3. Cập nhật trạng thái mới cho đơn hàng
        order.setStatus(targetStatus);
        Order updatedOrder = orderRepository.save(order);

        return mapToOrderResponse(updatedOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse cancelOrder(String orderCode, String performedBy) {
        // Hủy đơn hàng là trường hợp đặc biệt chuyển sang CANCELLED
        return updateOrderStatus(orderCode, UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CANCELLED)
                .build(), performedBy);
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
                        .costOfGoodsSold(item.getCostOfGoodsSold())
                        .grossProfit(item.getCostOfGoodsSold() == null
                                ? null
                                : item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()))
                                        .subtract(item.getCostOfGoodsSold()))
                        .costTracked(item.getCostOfGoodsSold() != null)
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser().getId())
                .username(order.getUser().getUsername())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void allocateFifoCost(OrderItem item) {
        List<InventoryBatch> batches = inventoryBatchRepository.findAvailableForUpdate(item.getProduct().getId());
        int remainingToShip = item.getQuantity();
        BigDecimal totalCost = BigDecimal.ZERO;
        boolean fullyCosted = true;

        for (InventoryBatch batch : batches) {
            if (remainingToShip == 0) break;
            int allocatedQuantity = Math.min(batch.getRemainingQuantity(), remainingToShip);
            batch.setRemainingQuantity(batch.getRemainingQuantity() - allocatedQuantity);
            remainingToShip -= allocatedQuantity;

            item.addBatchAllocation(OrderItemBatchAllocation.builder()
                    .inventoryBatch(batch)
                    .quantity(allocatedQuantity)
                    .unitCostAtShipment(batch.getUnitCost())
                    .build());

            if (batch.getUnitCost() == null) {
                fullyCosted = false;
            } else {
                totalCost = totalCost.add(batch.getUnitCost().multiply(BigDecimal.valueOf(allocatedQuantity)));
            }
        }

        if (remainingToShip > 0) {
            throw new AppException(ErrorCode.INVENTORY_BATCH_MISMATCH);
        }
        inventoryBatchRepository.saveAll(batches);
        item.setCostOfGoodsSold(fullyCosted ? totalCost : null);
    }

    private void restoreFifoBatches(OrderItem item) {
        if (item.getBatchAllocations().isEmpty()) {
            inventoryBatchRepository.save(InventoryBatch.builder()
                    .product(item.getProduct())
                    .batchCode("RETURN-LEGACY-" + item.getOrder().getOrderCode())
                    .receivedQuantity(item.getQuantity())
                    .remainingQuantity(item.getQuantity())
                    .unitCost(null)
                    .receivedAt(java.time.LocalDateTime.now())
                    .build());
            return;
        }

        item.getBatchAllocations().forEach(allocation -> {
            InventoryBatch batch = allocation.getInventoryBatch();
            batch.setRemainingQuantity(batch.getRemainingQuantity() + allocation.getQuantity());
            inventoryBatchRepository.save(batch);
        });
    }
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Sắp xếp mới nhất lên đầu
                .map(this::mapToOrderResponse)
                .toList();
    }
}