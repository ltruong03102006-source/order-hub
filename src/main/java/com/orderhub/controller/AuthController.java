package com.orderhub.controller;

import com.orderhub.dto.request.LoginRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.LoginResponse;
import com.orderhub.entity.enums.Role;
import com.orderhub.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "0. Xác thực tài khoản (Auth)", description = "Đăng nhập nhận Token JWT")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống (Test với tài khoản: admin/admin123 hoặc customer/customer123)")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        String role;
        if ("admin".equalsIgnoreCase(request.getUsername()) && "admin123".equals(request.getPassword())) {
            role = Role.ADMIN.name();
        } else if ("customer".equalsIgnoreCase(request.getUsername()) && "customer123".equals(request.getPassword())) {
            role = Role.CUSTOMER.name();
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<LoginResponse>builder()
                            .code(400)
                            .message("Tài khoản hoặc mật khẩu không chính xác")
                            .build()
            );
        }

        String token = tokenProvider.generateToken(request.getUsername(), role);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(request.getUsername())
                .role(role)
                .build();

        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .message("Đăng nhập thành công")
                .data(response)
                .build());
    }
}