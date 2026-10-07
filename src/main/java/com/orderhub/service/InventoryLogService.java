package com.orderhub.service;

import com.orderhub.dto.response.InventoryLogResponse;
import java.util.List;

public interface InventoryLogService {
    List<InventoryLogResponse> getAllLogs();
    List<InventoryLogResponse> getLogsByProduct(Long productId);
    List<InventoryLogResponse> getLogsByOrderCode(String orderCode);
}