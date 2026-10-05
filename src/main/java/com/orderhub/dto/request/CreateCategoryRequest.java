package com.orderhub.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCategoryRequest {

    @Schema(description = "Tên danh mục hiển thị", example = "Thiết bị điện tử")
    @NotBlank(message = "Tên danh mục không được để trống")
    String name;

    @Schema(description = "Mã định danh danh mục (duy nhất)", example = "ELECTRONICS")
    @NotBlank(message = "Mã danh mục không được để trống")
    String code;

    @Schema(description = "Mô tả ngắn gọn", example = "Điện thoại, phụ kiện, máy tính")
    String description;
}