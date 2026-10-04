package com.orderhub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportStockRequest {

    @Schema(description = "ID của sản phẩm cần nhập kho", example = "1")
    @NotNull(message = "Product ID không được để trống")
    Long productId;

    @Schema(description = "Số lượng nhập thêm", example = "50")
    @NotNull(message = "Số lượng nhập không được để trống")
    @Min(value = 1, message = "Số lượng nhập tối thiểu là 1")
    Integer quantity;

    @Schema(description = "Đơn giá nhập hàng từ nhà cung cấp (VNĐ)", example = "150000")
    BigDecimal costPrice;

    @Schema(description = "Mã lô hàng / Số hóa đơn nhà cung cấp", example = "PO-202610-01")
    String batchCode;

    @Schema(description = "Ghi chú lô hàng", example = "Nhập bổ sung đợt 1 tháng 10")
    String note;
}