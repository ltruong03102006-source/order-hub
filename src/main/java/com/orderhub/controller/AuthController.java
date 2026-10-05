package com.orderhub.controller;

import com.orderhub.dto.request.LoginRequest;
import com.orderhub.dto.request.RegisterRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.LoginResponse;
import com.orderhub.entity.User;
import com.orderhub.entity.enums.Role;
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

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản (Mặc định Role: CUSTOMER)")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<String>builder()
                            .code(400)
                            .message("Tên đăng nhập đã tồn tại")
                            .build()
            );
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<String>builder()
                        .code(201)
                        .message("Đăng ký tài khoản thành công")
                        .data(newUser.getUsername())
                        .build()
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập xác thực từ CSDL và nhận JWT Token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .code(200)
                .message("Đăng nhập thành công")
                .data(response)
                .build());
    }
}