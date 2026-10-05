package com.orderhub.repository;

import com.orderhub.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    // Dùng để dọn dẹp các key hết hạn sau này
    void deleteAllByCreatedAtBefore(LocalDateTime expiryTime);
}