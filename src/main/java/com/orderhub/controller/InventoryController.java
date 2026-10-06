package com.orderhub.controller;

import com.orderhub.dto.request.ImportStockRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.InventoryResponse;
import com.orderhub.entity.InventoryLog;
import com.orderhub.repository.InventoryLogRepository;
import com.orderhub.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
@Tag(name = "3. Quản Lý Tồn Kho (Inventory Engine)", description = "Nhập hàng từ nhà cung cấp (Inbound) và tra cứu số lượng khả dụng")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryLogRepository inventoryLogRepository; // Thêm repository này

    @PostMapping("/import")
    @Operation(
            summary = "Nhập kho ban đầu / Bổ sung hàng (Inbound Stock Import)",
            description = "Tăng total_quantity cho sản phẩm và ghi nhận bản ghi Audit Log có actionType = IMPORT."
    )
    public ResponseEntity<ApiResponse<InventoryResponse>> importStock(@Valid @RequestBody ImportStockRequest request) {
        return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                .message("Nhập kho thành công")
                .data(inventoryService.importStock(request))
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

    // THÊM ENDPOINT NÀY ĐỂ FRONTEND LẤY DANH SÁCH AUDIT LOG
    @GetMapping("/logs")
    @Operation(summary = "Lấy toàn bộ lịch sử biến động kho (Audit Logs)")
    public ResponseEntity<ApiResponse<List<InventoryLog>>> getAllInventoryLogs() {
        return ResponseEntity.ok(ApiResponse.<List<InventoryLog>>builder()
                .message("Lấy lịch sử kho thành công")
                .data(inventoryLogRepository.findAll(Sort.by(Sort.Direction.DESC, "id")))
                .build());
    }
}