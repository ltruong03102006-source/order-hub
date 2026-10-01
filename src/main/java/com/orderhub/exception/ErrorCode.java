package com.orderhub.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(1001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(1002, "Sản phẩm không tồn tại", HttpStatus.NOT_FOUND),
    OUT_OF_STOCK(1003, "Sản phẩm trong kho không đủ số lượng", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1004, "Đơn hàng không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_STATUS_TRANSITION(1005, "Không thể chuyển sang trạng thái đơn hàng này", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1006, "Yêu cầu đăng nhập hoặc token không hợp lệ", HttpStatus.UNAUTHORIZED),
    INVALID_INPUT(9998, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi hệ thống không xác định", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}