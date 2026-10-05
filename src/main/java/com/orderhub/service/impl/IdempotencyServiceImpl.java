package com.orderhub.service.impl;

import com.orderhub.entity.IdempotencyRecord;
import com.orderhub.entity.enums.IdempotencyStatus;
import com.orderhub.repository.IdempotencyRecordRepository;
import com.orderhub.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> getRecord(String key) {
        return idempotencyRecordRepository.findByIdempotencyKey(key);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void lockKey(String key) {
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(key)
                    .status(IdempotencyStatus.PROCESSING)
                    .build();
            idempotencyRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Idempotency key [{}] đã tồn tại trong database", key);
            throw ex;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSuccessResponse(String key, String responseJson) {
        idempotencyRecordRepository.findByIdempotencyKey(key).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponseBody(responseJson);
            idempotencyRecordRepository.save(record);
            log.info("Đã cache kết quả đơn hàng cho Idempotency-Key [{}]", key);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlockKey(String key) {
        idempotencyRecordRepository.findByIdempotencyKey(key)
                .ifPresent(idempotencyRecordRepository::delete);
    }
}