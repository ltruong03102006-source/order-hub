package com.orderhub.dto.response;

import com.orderhub.entity.enums.InventoryActionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryLogResponse {
    Long id;
    Long productId;
    Integer changeAmount;
    InventoryActionType actionType;
    String referenceOrderCode;
    String note;
    LocalDateTime createdAt;
}