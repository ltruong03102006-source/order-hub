package com.orderhub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderhub.dto.request.CreateOrderRequest;
import com.orderhub.dto.request.UpdateOrderStatusRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.OrderResponse;
import com.orderhub.entity.IdempotencyRecord;
import com.orderhub.entity.enums.IdempotencyStatus;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.service.IdempotencyService;
import com.orderhub.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "1. Quản Lý Đơn Hàng (Order Engine)", description = "Tạo đơn hàng, khóa giữ kho và quản lý vòng đời đơn")
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(
            summary = "Tạo đơn hàng mới (Hỗ trợ chống trùng lặp Idempotency)",
            description = "Truyền kèm header X-Idempotency-Key (UUID). Nếu gửi lại cùng một key, hệ thống trả về kết quả cũ mà không trừ kho lần hai."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) throws Exception {

        // 1. Nếu client không truyền header Idempotency Key -> Xử lý tạo đơn bình thường
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.<OrderResponse>builder()
                            .code(HttpStatus.CREATED.value())
                            .message("Tạo đơn hàng thành công")
                            .data(response)
                            .build()
            );
        }

        // 2. Nếu có truyền key -> Kiểm tra xem đã xử lý trước đó chưa
        Optional<IdempotencyRecord> existingRecord = idempotencyService.getRecord(idempotencyKey);
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();

            // Nếu đang trong tiến trình xử lý đơn lần 1 -> Báo xung đột 409
            if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new AppException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }

            // Nếu đơn lần 1 đã hoàn thành -> Lấy JSON cache trả về ngay
            if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getResponseBody() != null) {
                OrderResponse cachedResponse = objectMapper.readValue(record.getResponseBody(), OrderResponse.class);
                return ResponseEntity.ok(
                        ApiResponse.<OrderResponse>builder()
                                .code(HttpStatus.OK.value())
                                .message("Đơn hàng đã được tạo trước đó (Phản hồi từ Cache Idempotency)")
                                .data(cachedResponse)
                                .build()
                );
            }
        }

        // 3. Khóa key lại để xử lý tạo đơn lần đầu
        try {
            idempotencyService.lockKey(idempotencyKey);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        try {
            // Chạy luồng tạo đơn và giữ tồn kho
            OrderResponse response = orderService.createOrder(request);

            // Lưu kết quả JSON lại vào database để phục vụ các lần retry tiếp theo
            String jsonResult = objectMapper.writeValueAsString(response);
            idempotencyService.saveSuccessResponse(idempotencyKey, jsonResult);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.<OrderResponse>builder()
                            .code(HttpStatus.CREATED.value())
                            .message("Tạo đơn hàng thành công")
                            .data(response)
                            .build()
            );
        } catch (Exception ex) {
            // Nếu tạo đơn thất bại do hết tồn kho hoặc lỗi logic -> Nhả key để người dùng có thể gửi lại
            idempotencyService.unlockKey(idempotencyKey);
            throw ex;
        }
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