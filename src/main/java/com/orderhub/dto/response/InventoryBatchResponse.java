package com.orderhub.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryBatchResponse {
    private Long id;
    private Long productId;
    private String batchCode;
    private Integer receivedQuantity;
    private Integer remainingQuantity;
    private BigDecimal unitCost;
    private BigDecimal remainingStockValue;
    private LocalDateTime receivedAt;
    private Boolean costTracked;
}
