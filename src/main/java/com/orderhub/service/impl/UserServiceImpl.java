package com.orderhub.service.impl;

import com.orderhub.dto.request.CreateStaffRequest;
import com.orderhub.dto.response.UserResponse;
import com.orderhub.entity.User;
import com.orderhub.entity.enums.Role;
import com.orderhub.exception.AppException;
import com.orderhub.exception.ErrorCode;
import com.orderhub.repository.UserRepository;
import com.orderhub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User staff = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(Role.STAFF)
                .active(true)
                .build();

        User saved = userRepository.save(staff);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllStaffs() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STAFF)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void toggleStaffStatus(Long staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        staff.setActive(!Boolean.TRUE.equals(staff.getActive()));
        userRepository.save(staff);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}