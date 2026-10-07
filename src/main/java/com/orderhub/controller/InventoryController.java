package com.orderhub.controller;

import com.orderhub.dto.request.ImportStockRequest;
import com.orderhub.dto.request.SetOpeningBatchCostRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.InventoryResponse;
import com.orderhub.dto.response.InventoryBatchResponse;
import com.orderhub.dto.response.InventoryLogResponse;
import com.orderhub.service.InventoryLogService;
import com.orderhub.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
@Tag(name = "3. Quản Lý Tồn Kho (Inventory Engine)", description = "Nhập hàng từ nhà cung cấp (Inbound) và tra cứu số lượng khả dụng")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryLogService inventoryLogService;

    @PostMapping("/import")
    @Operation(
            summary = "Nhập kho ban đầu / Bổ sung hàng (Inbound Stock Import)",
            description = "Tăng total_quantity cho sản phẩm và ghi nhận bản ghi Audit Log có actionType = IMPORT."
    )
    public ResponseEntity<ApiResponse<InventoryResponse>> importStock(
            @Valid @RequestBody ImportStockRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                .message("Nhập kho thành công")
                .data(inventoryService.importStock(request, authentication.getName()))
                .build());
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Xem số lượng tồn kho theo Sản phẩm")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                .message("Lấy thông tin tồn kho thành công")
                .data(inventoryService.getInventoryByProductId(productId))
                .build());
    }

    @GetMapping("/product/{productId}/batches")
    @Operation(summary = "Xem các lô nhập và giá vốn FIFO theo sản phẩm")
    public ResponseEntity<ApiResponse<List<InventoryBatchResponse>>> getBatchesByProduct(
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.<List<InventoryBatchResponse>>builder()
                .message("Lấy danh sách lô nhập thành công")
                .data(inventoryService.getBatchesByProductId(productId))
                .build());
    }

    @PatchMapping("/product/{productId}/opening-cost")
    @Operation(summary = "Gán giá vốn cho lô tồn đầu kỳ cũ chưa được định giá")
    public ResponseEntity<ApiResponse<InventoryBatchResponse>> setOpeningBatchCost(
            @PathVariable Long productId,
            @Valid @RequestBody SetOpeningBatchCostRequest request) {
        return ResponseEntity.ok(ApiResponse.<InventoryBatchResponse>builder()
                .message("Đã cập nhật giá vốn tồn đầu kỳ")
                .data(inventoryService.setOpeningBatchCost(productId, request))
                .build());
    }

    @GetMapping("/logs")
    @Operation(summary = "Lấy toàn bộ lịch sử biến động kho (Audit Logs)")
    public ResponseEntity<ApiResponse<List<InventoryLogResponse>>> getAllInventoryLogs() {
        return ResponseEntity.ok(ApiResponse.<List<InventoryLogResponse>>builder()
                .message("Lấy lịch sử kho thành công")
                .data(inventoryLogService.getAllLogs())
                .build());
    }
}