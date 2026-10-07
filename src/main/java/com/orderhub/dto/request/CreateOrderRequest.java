package com.orderhub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {

    @Schema(description = "Tương thích client cũ; không dùng làm người tạo đơn")
    Long userId;

    @Schema(description = "Danh sách sản phẩm trong giỏ hàng")
    @Valid
    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    List<OrderItemRequest> items;

    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự")
    String receiverName;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    String receiverPhone;

    @Size(max = 500, message = "Địa chỉ giao hàng tối đa 500 ký tự")
    String shippingAddress;
}