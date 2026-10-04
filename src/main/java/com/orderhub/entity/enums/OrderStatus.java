package com.orderhub.entity.enums;

import java.util.List;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    RETURNED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) return false;

        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED || target == RETURNED; // Đích đến từ SHIPPED
            case DELIVERED, RETURNED, CANCELLED -> false; // Các trạng thái cuối cùng, không thể đổi nữa
        };
    }
}