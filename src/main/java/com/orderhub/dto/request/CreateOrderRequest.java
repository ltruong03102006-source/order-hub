package com.orderhub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {

    @Schema(description = "ID của khách hàng đặt đơn", example = "1")
    @NotNull(message = "User ID không được để trống")
    Long userId;

    @Schema(description = "Danh sách sản phẩm trong giỏ hàng")
    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    List<OrderItemRequest> items;
}