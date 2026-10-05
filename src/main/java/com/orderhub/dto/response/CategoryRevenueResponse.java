package com.orderhub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRevenueResponse {

    @Schema(description = "Tên danh mục", example = "Thiết bị điện tử")
    String categoryName;

    @Schema(description = "Tổng số lượng đã bán", example = "45")
    Long totalSoldQuantity;

    @Schema(description = "Tổng doanh thu (VNĐ)", example = "45000000")
    BigDecimal totalRevenue;
}