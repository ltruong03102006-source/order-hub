package com.orderhub.service;

import com.orderhub.dto.request.ImportStockRequest;
import com.orderhub.dto.request.SetOpeningBatchCostRequest;
import com.orderhub.dto.response.InventoryResponse;
import com.orderhub.dto.response.InventoryBatchResponse;

import java.util.List;
public interface InventoryService {
    InventoryResponse importStock(ImportStockRequest request, String performedBy);
    InventoryResponse getInventoryByProductId(Long productId);
    List<InventoryBatchResponse> getBatchesByProductId(Long productId);
    InventoryBatchResponse setOpeningBatchCost(Long productId, SetOpeningBatchCostRequest request);
}