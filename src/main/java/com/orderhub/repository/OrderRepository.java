package com.orderhub.repository;

import com.orderhub.entity.Order;
import com.orderhub.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByUserId(Long userId);

    // Truy vấn các đơn hàng ở trạng thái PENDING tạo trước một mốc thời gian
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt <= :cutoffTime")
    List<Order> findExpiredOrders(
            @Param("status") OrderStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );
}