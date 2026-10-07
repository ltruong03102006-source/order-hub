package com.orderhub.service;

import com.orderhub.dto.request.CreateStaffRequest;
import com.orderhub.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createStaff(CreateStaffRequest request);
    List<UserResponse> getAllStaffs();
    void toggleStaffStatus(Long staffId);
}