package com.orderhub.controller;

import com.orderhub.dto.request.LoginRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.LoginResponse;
import com.orderhub.entity.User;
import com.orderhub.repository.UserRepository;
import com.orderhub.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "0. Xác thực tài khoản (Auth)", description = "Đăng ký & Đăng nhập")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập xác thực từ CSDL và nhận JWT Token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null
                || !Boolean.TRUE.equals(user.getActive())
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ApiResponse.<LoginResponse>builder()
                            .code(401)
                            .message("Tài khoản hoặc mật khẩu không chính xác")
                            .build()
            );
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .code(200)
                .message("Đăng nhập thành công")
                .data(response)
                .build());
    }
}