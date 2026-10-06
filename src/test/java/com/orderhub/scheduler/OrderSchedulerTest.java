package com.orderhub.scheduler;

import com.orderhub.entity.InventoryLog;
import com.orderhub.entity.Order;
import com.orderhub.entity.OrderItem;
import com.orderhub.entity.Product;
import com.orderhub.entity.enums.InventoryActionType;
import com.orderhub.entity.enums.OrderStatus;
import com.orderhub.repository.IdempotencyRecordRepository;
import com.orderhub.repository.InventoryLogRepository;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private InventoryLogRepository inventoryLogRepository;

    private OrderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OrderScheduler(
                orderRepository,
                inventoryRepository,
                idempotencyRecordRepository,
                inventoryLogRepository
        );
        ReflectionTestUtils.setField(scheduler, "expirationMinutes", 15);
    }

    @Test
    void expiredOrderReleaseCreatesInventoryLog() {
        Order order = expiredOrder();
        when(orderRepository.findExpiredOrders(eq(OrderStatus.PENDING), any())).thenReturn(List.of(order));
        when(inventoryRepository.releaseStock(42L, 3)).thenReturn(1);

        scheduler.scanAndCancelExpiredOrders();

        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        InventoryLog inventoryLog = logCaptor.getValue();
        assertEquals(42L, inventoryLog.getProductId());
        assertEquals(-3, inventoryLog.getChangeAmount());
        assertEquals(InventoryActionType.RELEASE, inventoryLog.getActionType());
        assertEquals("ORD-EXPIRED", inventoryLog.getReferenceOrderCode());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void failedReservationReleaseDoesNotCreateInventoryLog() {
        Order order = expiredOrder();
        when(orderRepository.findExpiredOrders(eq(OrderStatus.PENDING), any())).thenReturn(List.of(order));
        when(inventoryRepository.releaseStock(42L, 3)).thenReturn(0);

        scheduler.scanAndCancelExpiredOrders();

        verify(inventoryLogRepository, never()).save(any(InventoryLog.class));
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    private Order expiredOrder() {
        Product product = Product.builder().id(42L).build();
        OrderItem item = OrderItem.builder().product(product).quantity(3).build();
        return Order.builder()
                .orderCode("ORD-EXPIRED")
                .status(OrderStatus.PENDING)
                .items(List.of(item))
                .build();
    }
}
