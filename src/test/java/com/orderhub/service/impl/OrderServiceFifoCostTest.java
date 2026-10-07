package com.orderhub.service.impl;

import com.orderhub.dto.request.UpdateOrderStatusRequest;
import com.orderhub.entity.InventoryBatch;
import com.orderhub.entity.InventoryLog;
import com.orderhub.entity.OrderItemBatchAllocation;
import com.orderhub.entity.Order;
import com.orderhub.entity.OrderItem;
import com.orderhub.entity.Product;
import com.orderhub.entity.User;
import com.orderhub.entity.enums.OrderStatus;
import com.orderhub.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceFifoCostTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryBatchRepository inventoryBatchRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private InventoryLogRepository inventoryLogRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shipmentAllocatesOldestLotsFirstAndCalculatesGrossProfit() {
        Product product = Product.builder().id(4L).sku("SKU-4").name("Product").price(new BigDecimal("30")).build();
        InventoryBatch firstLot = batch(product, "LOT-A", 2, 2, "10", 1);
        InventoryBatch secondLot = batch(product, "LOT-B", 5, 5, "15", 2);
        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(3)
                .priceAtPurchase(new BigDecimal("20"))
                .build();
        User user = User.builder().id(7L).username("staff").build();
        Order order = Order.builder()
                .id(9L)
                .orderCode("ORD-TEST")
                .user(user)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("60"))
                .items(List.of(item))
                .build();
        item.setOrder(order);

        when(orderRepository.findByOrderCode("ORD-TEST")).thenReturn(Optional.of(order));
        when(inventoryBatchRepository.findAvailableForUpdate(4L)).thenReturn(List.of(firstLot, secondLot));
        when(inventoryRepository.deductStockOnShipment(4L, 3)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.updateOrderStatus(
                "ORD-TEST",
                UpdateOrderStatusRequest.builder().status(OrderStatus.SHIPPED).build(),
                "staff");

        assertEquals(0, firstLot.getRemainingQuantity());
        assertEquals(4, secondLot.getRemainingQuantity());
        assertEquals(new BigDecimal("35"), item.getCostOfGoodsSold());
        assertEquals(new BigDecimal("25"), response.getItems().get(0).getGrossProfit());
        assertEquals("LOT-A", item.getBatchAllocations().get(0).getInventoryBatch().getBatchCode());
        assertEquals("LOT-B", item.getBatchAllocations().get(1).getInventoryBatch().getBatchCode());
        verify(inventoryBatchRepository).saveAll(List.of(firstLot, secondLot));
        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        assertEquals("staff", logCaptor.getValue().getPerformedBy());
    }

    @Test
    void returnedDeliveredOrderRestocksTheOriginalLots() {
        Product product = Product.builder().id(4L).sku("SKU-4").name("Product").build();
        InventoryBatch firstLot = batch(product, "LOT-A", 2, 0, "10", 1);
        InventoryBatch secondLot = batch(product, "LOT-B", 5, 4, "15", 2);
        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(3)
                .priceAtPurchase(new BigDecimal("20"))
                .build();
        item.addBatchAllocation(OrderItemBatchAllocation.builder()
                .inventoryBatch(firstLot).quantity(2).unitCostAtShipment(new BigDecimal("10")).build());
        item.addBatchAllocation(OrderItemBatchAllocation.builder()
                .inventoryBatch(secondLot).quantity(1).unitCostAtShipment(new BigDecimal("15")).build());
        Order order = Order.builder()
                .id(9L)
                .orderCode("ORD-RETURN")
                .user(User.builder().id(7L).username("staff").build())
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("60"))
                .items(List.of(item))
                .build();
        item.setOrder(order);

        when(orderRepository.findByOrderCode("ORD-RETURN")).thenReturn(Optional.of(order));
        when(inventoryRepository.restockOnReturn(4L, 3)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateOrderStatus(
                "ORD-RETURN",
                UpdateOrderStatusRequest.builder().status(OrderStatus.RETURNED).build(),
                "staff");

        assertEquals(2, firstLot.getRemainingQuantity());
        assertEquals(5, secondLot.getRemainingQuantity());
    }

    private InventoryBatch batch(Product product, String code, int received, int remaining,
                                 String cost, int day) {
        return InventoryBatch.builder()
                .id((long) day)
                .product(product)
                .batchCode(code)
                .receivedQuantity(received)
                .remainingQuantity(remaining)
                .unitCost(new BigDecimal(cost))
                .receivedAt(LocalDateTime.of(2026, 1, day, 0, 0))
                .build();
    }
}
