package com.orderhub.entity.enums;

public enum OrderStatus {
    PENDING {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return next == CONFIRMED || next == CANCELLED;
        }
    },
    CONFIRMED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return next == SHIPPED || next == CANCELLED;
        }
    },
    SHIPPED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return false; // Đã giao hàng thì không đổi được nữa
        }
    },
    CANCELLED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return false; // Đã hủy thì kết thúc luồng
        }
    };

    public boolean canTransitionTo(OrderStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == CONFIRMED || nextStatus == CANCELLED;
            case CONFIRMED -> nextStatus == SHIPPED || nextStatus == CANCELLED;
            case SHIPPED, CANCELLED -> false; // Trạng thái kết thúc, không thể đổi
        };
    }
}