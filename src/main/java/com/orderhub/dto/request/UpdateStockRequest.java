package com.orderhub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateStockRequest {

    @NotNull(message = "Số lượng cập nhật không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    Integer quantity;
}