package com.orderhub.dto.request;

import com.orderhub.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateOrderStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    OrderStatus status;
}