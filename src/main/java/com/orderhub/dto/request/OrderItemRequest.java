package com.orderhub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemRequest {

    @Schema(description = "ID sản phẩm cần mua", example = "1")
    @NotNull
    Long productId;

    @Schema(description = "Số lượng mua", example = "1")
    @NotNull
    @Min(1)
    Integer quantity;
}