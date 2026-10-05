package com.orderhub.service;

import com.orderhub.entity.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyService {
    Optional<IdempotencyRecord> getRecord(String key);
    void lockKey(String key);
    void saveSuccessResponse(String key, String responseJson);
    void unlockKey(String key); // Dùng khi có lỗi xảy ra cần nhả key
}