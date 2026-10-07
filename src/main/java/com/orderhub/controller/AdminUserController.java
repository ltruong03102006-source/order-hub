package com.orderhub.controller;

import com.orderhub.dto.request.CreateStaffRequest;
import com.orderhub.dto.response.ApiResponse;
import com.orderhub.dto.response.UserResponse;
import com.orderhub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/staffs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới có quyền tạo và quản lý nhân viên
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        UserResponse res = userService.createStaff(request);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .message("Tạo tài khoản nhân viên thành công")
                .data(res)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllStaffs() {
        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .message("Lấy danh sách nhân viên thành công")
                .data(userService.getAllStaffs())
                .build());
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleStaffStatus(@PathVariable Long id) {
        userService.toggleStaffStatus(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Cập nhật trạng thái tài khoản thành công")
                .build());
    }
}