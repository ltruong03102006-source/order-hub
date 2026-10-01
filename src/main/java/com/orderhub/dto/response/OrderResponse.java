package com.orderhub.dto.response;

import com.orderhub.entity.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    Long id;
    String orderCode;
    Long userId;
    String username;
    OrderStatus status;
    BigDecimal totalAmount;
    List<OrderItemResponse> items;
    LocalDateTime createdAt;
}