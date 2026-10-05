package com.orderhub.config;

import com.orderhub.entity.User;
import com.orderhub.entity.enums.Role;
import com.orderhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Quản Trị Viên")
                    .email("admin@orderhub.com") // Bổ sung dòng này
                    .role(Role.ADMIN)
                    .build();

            User customer = User.builder()
                    .username("customer")
                    .password(passwordEncoder.encode("customer123"))
                    .fullName("Khách Hàng Mẫu")
                    .email("customer@orderhub.com") // Bổ sung dòng này
                    .role(Role.CUSTOMER)
                    .build();

            userRepository.saveAll(List.of(admin, customer));
        }
    }
}