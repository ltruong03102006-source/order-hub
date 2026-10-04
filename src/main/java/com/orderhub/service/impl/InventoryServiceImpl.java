package com.orderhub.service.impl;

import com.orderhub.dto.request.ImportStockRequest;
import com.orderhub.dto.response.InventoryResponse;
import com.orderhub.entity.Inventory;
import com.orderhub.entity.InventoryLog;
import com.orderhub.entity.Product;
import com.orderhub.entity.enums.InventoryActionType;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.InventoryLogRepository;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.ProductRepository;
import com.orderhub.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryResponse importStock(ImportStockRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElseGet(() -> {
                    // Nếu sản phẩm mới chưa có kho, khởi tạo mới
                    return inventoryRepository.save(Inventory.builder()
                            .product(product)
                            .totalQuantity(0)
                            .reservedQuantity(0)
                            .build());
                });

        // 1. Tăng tồn kho nguyên tử
        inventoryRepository.importStock(product.getId(), request.getQuantity());

        // 2. Ghi Audit Log (Stock Ledger)
        String logNote = String.format("Nhập kho từ lô hàng: %s. %s",
                request.getBatchCode() != null ? request.getBatchCode() : "N/A",
                request.getNote() != null ? request.getNote() : "");

        InventoryLog auditLog = InventoryLog.builder()
                .productId(product.getId())
                .changeAmount(request.getQuantity()) // Số dương
                .actionType(InventoryActionType.IMPORT)
                .referenceOrderCode(request.getBatchCode() != null ? request.getBatchCode() : "IMPORT-BATCH")
                .note(logNote.trim())
                .build();
        inventoryLogRepository.save(auditLog);

        log.info("Nhập kho thành công cho sản phẩm {}: +{} cái (Lô: {})",
                product.getId(), request.getQuantity(), request.getBatchCode());

        // 3. Lấy lại trạng thái kho mới nhất để trả về
        Inventory refreshedInventory = inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return mapToResponse(refreshedInventory);
    }

    @Override
    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return mapToResponse(inventory);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        int available = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .totalQuantity(inventory.getTotalQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(Math.max(available, 0))
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}