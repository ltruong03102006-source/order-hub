package com.orderhub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryStockResponse {

    @Schema(description = "Tên danh mục", example = "Thiết bị điện tử")
    String categoryName;

    @Schema(description = "Số lượng mã sản phẩm trong ngành hàng", example = "10")
    Long productCount;

    @Schema(description = "Tổng tồn kho thực tế (total_quantity)", example = "200")
    Long totalStock;

    @Schema(description = "Lượng đang bị giữ chỗ (reserved_quantity)", example = "15")
    Long totalReserved;
}