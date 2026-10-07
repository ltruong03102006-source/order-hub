package com.orderhub.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SetOpeningBatchCostRequest {

    @NotNull(message = "Giá vốn tồn đầu kỳ không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá vốn phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá vốn tối đa 10 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal unitCost;
}
