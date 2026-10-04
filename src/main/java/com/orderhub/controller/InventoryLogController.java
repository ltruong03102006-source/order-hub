package com.orderhub.controller;

import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.InventoryLogResponse;
import com.orderhub.service.InventoryLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Inventory Log Controller", description = "Truy vấn lịch sử và biến động tồn kho (Stock Ledger)")
public class InventoryLogController {

    InventoryLogService inventoryLogService;

    @GetMapping("/product/{productId}")
    @Operation(summary = "Xem lịch sử biến động kho theo Sản phẩm")
    public ResponseEntity<ApiResponse<List<InventoryLogResponse>>> getLogsByProduct(@PathVariable Long productId) {
        List<InventoryLogResponse> logs = inventoryLogService.getLogsByProduct(productId);
        return ResponseEntity.ok(ApiResponse.<List<InventoryLogResponse>>builder()
                .message("Lấy lịch sử biến động kho thành công")
                .data(logs)
                .build());
    }

    @GetMapping("/order/{orderCode}")
    @Operation(summary = "Xem lịch sử biến động kho theo Mã đơn hàng")
    public ResponseEntity<ApiResponse<List<InventoryLogResponse>>> getLogsByOrder(@PathVariable String orderCode) {
        List<InventoryLogResponse> logs = inventoryLogService.getLogsByOrderCode(orderCode);
        return ResponseEntity.ok(ApiResponse.<List<InventoryLogResponse>>builder()
                .message("Lấy lịch sử biến động theo đơn hàng thành công")
                .data(logs)
                .build());
    }
}