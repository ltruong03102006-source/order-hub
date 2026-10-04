package com.orderhub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryResponse {

    @Schema(description = "ID bản ghi tồn kho", example = "1")
    Long id;

    @Schema(description = "ID sản phẩm", example = "1")
    Long productId;

    @Schema(description = "Tổng số lượng thực tế trong kho", example = "150")
    Integer totalQuantity;

    @Schema(description = "Số lượng đang bị khách giữ chỗ", example = "10")
    Integer reservedQuantity;

    @Schema(description = "Số lượng khả dụng để bán (total - reserved)", example = "140")
    Integer availableQuantity;

    @Schema(description = "Thời gian cập nhật gần nhất")
    LocalDateTime updatedAt;
}