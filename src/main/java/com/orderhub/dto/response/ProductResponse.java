package com.orderhub.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {
    Long id;
    String sku;
    String name;
    BigDecimal price;
    Integer stockQuantity;
    Long categoryId;
    String categoryName;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}