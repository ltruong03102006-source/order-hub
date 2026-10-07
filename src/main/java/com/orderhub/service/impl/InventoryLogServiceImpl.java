package com.orderhub.service.impl;

import com.orderhub.dto.response.InventoryLogResponse;
import com.orderhub.entity.InventoryLog;
import com.orderhub.repository.InventoryLogRepository;
import com.orderhub.service.InventoryLogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryLogServiceImpl implements InventoryLogService {

    InventoryLogRepository inventoryLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLogResponse> getAllLogs() {
        return inventoryLogRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLogResponse> getLogsByProduct(Long productId) {
        return inventoryLogRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLogResponse> getLogsByOrderCode(String orderCode) {
        return inventoryLogRepository.findByReferenceOrderCode(orderCode)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private InventoryLogResponse mapToResponse(InventoryLog log) {
        return InventoryLogResponse.builder()
                .id(log.getId())
                .productId(log.getProductId())
                .changeAmount(log.getChangeAmount())
                .actionType(log.getActionType())
                .referenceOrderCode(log.getReferenceOrderCode())
                .note(log.getNote())
                .performedBy(log.getPerformedBy())
                .createdAt(log.getCreatedAt())
                .build();
    }
}