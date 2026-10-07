package com.orderhub.service.impl;

import com.orderhub.dto.request.ImportStockRequest;
import com.orderhub.dto.request.SetOpeningBatchCostRequest;
import com.orderhub.dto.response.InventoryResponse;
import com.orderhub.dto.response.InventoryBatchResponse;
import com.orderhub.entity.Inventory;
import com.orderhub.entity.InventoryLog;
import com.orderhub.entity.InventoryBatch;
import com.orderhub.entity.Product;
import com.orderhub.entity.enums.InventoryActionType;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.InventoryLogRepository;
import com.orderhub.repository.InventoryBatchRepository;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.ProductRepository;
import com.orderhub.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final InventoryBatchRepository inventoryBatchRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryResponse importStock(ImportStockRequest request, String performedBy) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        inventoryRepository.findByProductId(request.getProductId())
                .orElseGet(() -> {
                    // Nếu sản phẩm mới chưa có kho, khởi tạo mới
                    return inventoryRepository.save(Inventory.builder()
                            .product(product)
                            .totalQuantity(0)
                            .reservedQuantity(0)
                            .build());
                });

        // Persist the cost-bearing lot and aggregate quantity in the same transaction.
        inventoryBatchRepository.save(InventoryBatch.builder()
                .product(product)
                .batchCode(request.getBatchCode().trim())
                .receivedQuantity(request.getQuantity())
                .remainingQuantity(request.getQuantity())
                .unitCost(request.getCostPrice())
                .receivedAt(LocalDateTime.now())
                .build());

        if (inventoryRepository.importStock(product.getId(), request.getQuantity()) != 1) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. Ghi Audit Log (Stock Ledger)
        String logNote = String.format("Lô: %s. Số lượng: %d. Giá vốn/đv: %s VND. %s",
                request.getBatchCode() != null ? request.getBatchCode() : "N/A",
                request.getQuantity(),
                request.getCostPrice(),
                request.getNote() != null ? request.getNote() : "");

        InventoryLog auditLog = InventoryLog.builder()
                .productId(product.getId())
                .changeAmount(request.getQuantity()) // Số dương
                .actionType(InventoryActionType.IMPORT)
                .referenceOrderCode(request.getBatchCode())
                .note(logNote.trim())
                .performedBy(performedBy)
                .build();
        inventoryLogRepository.save(auditLog);

        log.info("Nhập kho thành công cho sản phẩm {}: +{} cái, giá vốn {} (Lô: {})",
                product.getId(), request.getQuantity(), request.getCostPrice(), request.getBatchCode());

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

    @Override
    @Transactional(readOnly = true)
    public List<InventoryBatchResponse> getBatchesByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return inventoryBatchRepository.findByProductIdOrderByReceivedAtAscIdAsc(productId).stream()
                .map(this::mapBatch)
                .toList();
    }

    @Override
    @Transactional
    public InventoryBatchResponse setOpeningBatchCost(Long productId, SetOpeningBatchCostRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        InventoryBatch batch = inventoryBatchRepository
                .findFirstByProductIdAndBatchCodeStartingWithAndUnitCostIsNullAndRemainingQuantityGreaterThanOrderByReceivedAtAsc(
                        productId, "OPENING-UNVALUED-", 0)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT));
        batch.setUnitCost(request.getUnitCost());
        return mapBatch(batch);
    }

    private InventoryBatchResponse mapBatch(InventoryBatch batch) {
        return InventoryBatchResponse.builder()
                .id(batch.getId())
                .productId(batch.getProduct().getId())
                .batchCode(batch.getBatchCode())
                .receivedQuantity(batch.getReceivedQuantity())
                .remainingQuantity(batch.getRemainingQuantity())
                .unitCost(batch.getUnitCost())
                .remainingStockValue(batch.getUnitCost() == null
                        ? null
                        : batch.getUnitCost().multiply(BigDecimal.valueOf(batch.getRemainingQuantity())))
                .receivedAt(batch.getReceivedAt())
                .costTracked(batch.getUnitCost() != null)
                .build();
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