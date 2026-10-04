package com.orderhub.repository;

import com.orderhub.entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<InventoryLog> findByReferenceOrderCode(String referenceOrderCode);
    Page<InventoryLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}