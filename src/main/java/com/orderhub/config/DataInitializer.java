package com.orderhub.config;

import com.orderhub.entity.Product;
import com.orderhub.entity.User;
import com.orderhub.entity.enums.Role;
import com.orderhub.repository.ProductRepository;
import com.orderhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password("admin123")
                    .email("admin@orderhub.com")
                    .role(Role.ROLE_ADMIN)
                    .build();

            User customer = User.builder()
                    .username("customer")
                    .password("customer123")
                    .email("customer@orderhub.com")
                    .role(Role.ROLE_CUSTOMER)
                    .build();

            userRepository.saveAll(List.of(admin, customer));
        }

        if (productRepository.count() == 0) {
            Product p1 = Product.builder()
                    .sku("SKU-IPHONE15")
                    .name("iPhone 15 Pro Max 256GB")
                    .price(new BigDecimal("29990000"))
                    .stockQuantity(10)
                    .build();

            Product p2 = Product.builder()
                    .sku("SKU-MACBOOK-M3")
                    .name("MacBook Pro M3 14 inch")
                    .price(new BigDecimal("39990000"))
                    .stockQuantity(5)
                    .build();

            productRepository.saveAll(List.of(p1, p2));
        }
    }
}