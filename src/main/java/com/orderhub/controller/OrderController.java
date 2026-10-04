package com.orderhub.controller;

import com.orderhub.dto.request.CreateOrderRequest;
import com.orderhub.dto.request.UpdateOrderStatusRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.OrderResponse;
import com.orderhub.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "1. Quản Lý Đơn Hàng (Order Engine)", description = "Tạo đơn hàng, khóa giữ kho và quản lý vòng đời đơn")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Tạo đơn hàng mới (Tự động trừ kho an toàn)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<OrderResponse>builder()
                        .code(HttpStatus.CREATED.value())
                        .message("Tạo đơn hàng thành công")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/{orderCode}")
    @Operation(summary = "Tra cứu đơn hàng qua mã Code (Ví dụ: ORD-A1B2C3D4)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(@PathVariable String orderCode) {
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .data(orderService.getOrderByCode(orderCode))
                        .build()
        );
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Lấy danh sách đơn hàng theo User ID")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(
                ApiResponse.<List<OrderResponse>>builder()
                        .data(orderService.getOrdersByUserId(userId))
                        .build()
        );
    }
    @PatchMapping("/{orderCode}/status")
    @Operation(
            summary = "Cập nhật trạng thái vòng đời đơn hàng",
            description = "Chuyển trạng thái theo State Machine. Nếu CANCELLED -> hoàn kho (RELEASE). Nếu SHIPPED -> trừ kho thực (SHIP_DEDUCT)"
    )
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderCode,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .message("Cập nhật trạng thái đơn hàng thành công")
                        .data(orderService.updateOrderStatus(orderCode, request))
                        .build()
        );
    }

    @PatchMapping("/{orderCode}/cancel")
    @Operation(summary = "Hủy đơn hàng (Tự động hoàn lại số lượng tồn kho)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable String orderCode) {
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .message("Hủy đơn hàng thành công")
                        .data(orderService.cancelOrder(orderCode))
                        .build()
        );
    }
}