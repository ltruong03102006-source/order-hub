package com.orderhub.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderCreatedEvent {
    Long orderId;
    String orderCode;
    Long userId;
    BigDecimal totalAmount;
    LocalDateTime createdAt;
}