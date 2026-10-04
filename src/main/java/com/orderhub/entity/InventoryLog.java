package com.orderhub.entity;

import com.orderhub.entity.enums.InventoryActionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_logs",
        indexes = {
                @Index(name = "idx_inv_log_product_id", columnList = "product_id"),
                @Index(name = "idx_inv_log_order_code", columnList = "reference_order_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "product_id", nullable = false)
    Long productId;

    @Column(name = "change_amount", nullable = false)
    Integer changeAmount; // Số dương hoặc âm (+/-)

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    InventoryActionType actionType;

    @Column(name = "reference_order_code", length = 50)
    String referenceOrderCode; // Mã đơn hàng liên quan (nếu có)

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;
}