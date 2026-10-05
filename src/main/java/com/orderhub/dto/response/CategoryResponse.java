package com.orderhub.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryResponse {

    @Schema(description = "ID danh mục", example = "1")
    Long id;

    @Schema(description = "Tên danh mục", example = "Thiết bị điện tử")
    String name;

    @Schema(description = "Mã danh mục", example = "ELECTRONICS")
    String code;

    @Schema(description = "Mô tả", example = "Điện thoại, phụ kiện, máy tính")
    String description;

    @Schema(description = "Ngày tạo")
    LocalDateTime createdAt;
}