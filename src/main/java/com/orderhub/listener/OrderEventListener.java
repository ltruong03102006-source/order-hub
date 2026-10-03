package com.orderhub.listener;

import com.orderhub.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class OrderEventListener {

    // Chỉ thực thi KHI VÀ CHỈ KHI transaction tạo đơn đã COMMIT thành công
    @Async("orderAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[Async-Worker] Bắt đầu xử lý tác vụ hậu cần cho đơn hàng: {} trên Thread: {}",
                event.getOrderCode(), Thread.currentThread().getName());

        try {
            // 1. Giả lập gửi email hóa đơn cho khách hàng (mất 2 giây)
            sendOrderConfirmationEmail(event);

            // 2. Giả lập đẩy thông báo sang hệ thống vận chuyển/bán hàng (mất 1.5 giây)
            notifyLogisticsPartner(event);

            log.info("[Async-Worker] Đã hoàn tất toàn bộ tác vụ hậu cần cho đơn: {}", event.getOrderCode());
        } catch (Exception e) {
            log.error("[Async-Worker] Thất bại khi xử lý hậu cần đơn hàng: {}", event.getOrderCode(), e);
        }
    }

    private void sendOrderConfirmationEmail(OrderCreatedEvent event) throws InterruptedException {
        log.info("[Email-Service] Đang kết nối SMTP gửi email xác nhận cho đơn {} (Tổng tiền: {} VNĐ)...",
                event.getOrderCode(), event.getTotalAmount());
        Thread.sleep(2000); // Giả lập độ trễ I/O 2 giây
        log.info("[Email-Service] Gửi email thành công tới User ID: {}", event.getUserId());
    }

    private void notifyLogisticsPartner(OrderCreatedEvent event) throws InterruptedException {
        log.info("[Logistics-Webhook] Đang phát webhook thông báo đơn {} sang hệ thống giao hàng...",
                event.getOrderCode());
        Thread.sleep(1500); // Giả lập độ trễ mạng 1.5 giây
        log.info("[Logistics-Webhook] Đơn vị vận chuyển đã nhận đơn: {}", event.getOrderCode());
    }
}