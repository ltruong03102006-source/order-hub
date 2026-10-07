package com.orderhub.service.impl;

import com.orderhub.dto.request.CreateProductRequest;
import com.orderhub.dto.request.UpdateProductRequest;
import com.orderhub.dto.request.UpdateStockRequest;
import com.orderhub.dto.response.ProductResponse;
import com.orderhub.entity.Category;
import com.orderhub.entity.Inventory;
import com.orderhub.entity.InventoryBatch;
import com.orderhub.entity.InventoryLog;
import com.orderhub.entity.Product;
import com.orderhub.entity.enums.InventoryActionType;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.CategoryRepository;
import com.orderhub.repository.InventoryBatchRepository;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.InventoryLogRepository;
import com.orderhub.repository.ProductRepository;
import com.orderhub.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        if (request.getStockQuantity() > 0 && request.getInitialCostPrice() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        if (productRepository.findBySku(request.getSku()).isPresent()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT));
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .price(request.getPrice())
                .category(category) // 1. Bổ sung gán category vào product ở đây
                .build();

        Inventory inventory = Inventory.builder()
                .product(product)
                .totalQuantity(request.getStockQuantity())
                .reservedQuantity(0)
                .build();

        product.setInventory(inventory);

        Product savedProduct = productRepository.save(product);
        if (request.getStockQuantity() > 0) {
            inventoryBatchRepository.save(InventoryBatch.builder()
                    .product(savedProduct)
                    .batchCode("OPENING-" + savedProduct.getSku())
                    .receivedQuantity(request.getStockQuantity())
                    .remainingQuantity(request.getStockQuantity())
                    .unitCost(request.getInitialCostPrice())
                    .receivedAt(LocalDateTime.now())
                    .build());
        }
        return mapToResponse(savedProduct, inventoryBatchRepository
                .findByProductIdOrderByReceivedAtAscIdAsc(savedProduct.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        Map<Long, List<InventoryBatch>> batchesByProduct = products.isEmpty()
                ? Map.of()
                : inventoryBatchRepository
                        .findByProductIdInOrderByProductIdAscReceivedAtAscIdAsc(
                                products.stream().map(Product::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(batch -> batch.getProduct().getId()));
        return products.stream()
                .map(product -> mapToResponse(
                        product,
                        batchesByProduct.getOrDefault(product.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return mapToResponse(product, inventoryBatchRepository
                .findByProductIdOrderByReceivedAtAscIdAsc(id));
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long id, UpdateStockRequest request, String performedBy) {
        Inventory inventory = inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (request.getQuantity() < inventory.getReservedQuantity()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        int difference = request.getQuantity() - inventory.getTotalQuantity();
        List<InventoryBatch> batches = inventoryBatchRepository.findAvailableForUpdate(id);
        if (difference > 0) {
            inventoryBatchRepository.save(InventoryBatch.builder()
                    .product(inventory.getProduct())
                    .batchCode("ADJUSTMENT-" + System.currentTimeMillis())
                    .receivedQuantity(difference)
                    .remainingQuantity(difference)
                    .unitCost(null)
                    .receivedAt(LocalDateTime.now())
                    .build());
        } else if (difference < 0) {
            int toRemove = -difference;
            for (InventoryBatch batch : batches) {
                int removed = Math.min(batch.getRemainingQuantity(), toRemove);
                batch.setRemainingQuantity(batch.getRemainingQuantity() - removed);
                toRemove -= removed;
                if (toRemove == 0) break;
            }
            if (toRemove > 0) throw new AppException(ErrorCode.INVENTORY_BATCH_MISMATCH);
            inventoryBatchRepository.saveAll(batches);
        }
        inventory.setTotalQuantity(request.getQuantity());
        inventoryRepository.save(inventory);
        if (difference != 0) {
            inventoryLogRepository.save(InventoryLog.builder()
                    .productId(id)
                    .changeAmount(difference)
                    .actionType(InventoryActionType.ADMIN_ADJUST)
                    .note("Điều chỉnh tồn kho thủ công")
                    .performedBy(performedBy)
                    .build());
        }

        return mapToResponse(inventory.getProduct(),
                inventoryBatchRepository.findByProductIdOrderByReceivedAtAscIdAsc(id));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + id));

        if (request.getSku() != null && !request.getSku().isBlank()) {
            product.setSku(request.getSku());
        }
        product.setName(request.getName());
        product.setPrice(request.getPrice());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        Product updatedProduct = productRepository.save(product);

        // Dùng hàm map response sẵn có của bạn hoặc builder:
        return mapToResponse(updatedProduct,
                inventoryBatchRepository.findByProductIdOrderByReceivedAtAscIdAsc(id));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy sản phẩm với id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product product, List<InventoryBatch> batches) {
        Inventory inv = product.getInventory();
        int available = (inv != null && inv.getAvailableQuantity() != null) ? inv.getAvailableQuantity() : 0;
        int reserved = (inv != null && inv.getReservedQuantity() != null) ? inv.getReservedQuantity() : 0;
        int total = (inv != null && inv.getTotalQuantity() != null) ? inv.getTotalQuantity() : (available + reserved);

        int batchQuantity = batches.stream().mapToInt(InventoryBatch::getRemainingQuantity).sum();
        boolean costTracked = batchQuantity == total && batches.stream()
                .allMatch(batch -> batch.getRemainingQuantity() == 0 || batch.getUnitCost() != null);
        BigDecimal inventoryValue = batches.stream()
                .filter(batch -> batch.getUnitCost() != null)
                .map(batch -> batch.getUnitCost().multiply(BigDecimal.valueOf(batch.getRemainingQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageCost = costTracked && total > 0
                ? inventoryValue.divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP)
                : null;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .price(product.getPrice())
                .costPrice(averageCost)
                .grossMargin(averageCost == null ? null : product.getPrice().subtract(averageCost))
                .costTracked(costTracked)
                .stockQuantity(available)
                .totalQuantity(total)
                .reservedQuantity(reserved)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}