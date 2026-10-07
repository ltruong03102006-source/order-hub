package com.orderhub.repository;

import com.orderhub.entity.InventoryBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    List<InventoryBatch> findByProductIdOrderByReceivedAtAscIdAsc(Long productId);

    List<InventoryBatch> findByProductIdInOrderByProductIdAscReceivedAtAscIdAsc(Collection<Long> productIds);

    boolean existsByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryBatch> findFirstByProductIdAndBatchCodeStartingWithAndUnitCostIsNullAndRemainingQuantityGreaterThanOrderByReceivedAtAsc(
            Long productId, String batchCodePrefix, Integer remainingQuantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.product.id = :productId " +
            "AND b.remainingQuantity > 0 ORDER BY b.receivedAt ASC, b.id ASC")
    List<InventoryBatch> findAvailableForUpdate(@Param("productId") Long productId);
}
