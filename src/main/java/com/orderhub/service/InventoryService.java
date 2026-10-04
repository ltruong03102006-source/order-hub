package com.orderhub.service;

import com.orderhub.dto.request.ImportStockRequest;
import com.orderhub.dto.response.InventoryResponse;

public interface InventoryService {
    InventoryResponse importStock(ImportStockRequest request);
    InventoryResponse getInventoryByProductId(Long productId);
}