package com.orderhub.scheduler;

import com.orderhub.entity.InventoryLog;
import com.orderhub.entity.Order;
import com.orderhub.entity.OrderItem;
import com.orderhub.entity.enums.InventoryActionType;
import com.orderhub.entity.enums.OrderStatus;
import com.orderhub.repository.IdempotencyRecordRepository;
import com.orderhub.repository.InventoryLogRepository;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.OrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class OrderScheduler {

    final OrderRepository orderRepository;
    final InventoryRepository inventoryRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    final InventoryLogRepository inventoryLogRepository;

    // Cho phép cấu hình thời gian hết hạn qua application.yml (mặc định 15 phút)
    @Value("${order.expiration-minutes:15}")
    int expirationMinutes;

    // Chạy ngầm định kỳ mỗi 60 giây (60000 ms)
    @Scheduled(fixedDelayString = "${order.scheduler-interval-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public void scanAndCancelExpiredOrders() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(expirationMinutes);

        List<Order> expiredOrders = orderRepository.findExpiredOrders(OrderStatus.PENDING, cutoffTime);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("[Scheduler] Tìm thấy {} đơn hàng PENDING đã quá hạn {} phút. Đang xử lý tự động hủy...",
                expiredOrders.size(), expirationMinutes);

        for (Order order : expiredOrders) {
            try {
                // 1. Hoàn lại toàn bộ tồn kho giữ chỗ (reserved stock)
                for (OrderItem item : order.getItems()) {
                    int releasedRows = inventoryRepository.releaseStock(item.getProduct().getId(), item.getQuantity());
                    if (releasedRows > 0) {
                        inventoryLogRepository.save(InventoryLog.builder()
                                .productId(item.getProduct().getId())
                                .changeAmount(-item.getQuantity())
                                .actionType(InventoryActionType.RELEASE)
                                .referenceOrderCode(order.getOrderCode())
                                .note("Tự động giải phóng tồn kho: Hết thời gian giữ chỗ đơn hàng ("
                                        + expirationMinutes + " phút)")
                                .performedBy("SYSTEM")
                                .build());
                        log.info("[Scheduler] Đã hoàn lại {} sản phẩm (ID: {}) từ đơn hàng {}",
                                item.getQuantity(), item.getProduct().getId(), order.getOrderCode());
                    } else {
                        log.warn("[Scheduler] Không tìm thấy lượng giữ chỗ để giải phóng cho sản phẩm {} của đơn hàng {}",
                                item.getProduct().getId(), order.getOrderCode());
                    }
                }

                // 2. Chuyển trạng thái đơn sang CANCELLED
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);

                log.info("[Scheduler] Đã tự động hủy thành công đơn hàng hết hạn: {}", order.getOrderCode());
            } catch (Exception e) {
                log.error("[Scheduler] Lỗi khi xử lý hủy đơn quá hạn: {}", order.getOrderCode(), e);
            }
        }
    }
    @Scheduled(cron = "0 */10 * * * *") // Chạy mỗi 10 phút
    @Transactional
    public void cleanExpiredIdempotencyKeys() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        idempotencyRecordRepository.deleteAllByCreatedAtBefore(tenMinutesAgo);
    }
}