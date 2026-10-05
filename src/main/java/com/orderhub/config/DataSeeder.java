package com.orderhub.config;

import com.orderhub.entity.Category;
import com.orderhub.entity.Inventory;
import com.orderhub.entity.Product;
import com.orderhub.entity.User;
import com.orderhub.entity.enums.Role;
import com.orderhub.repository.CategoryRepository;
import com.orderhub.repository.InventoryRepository;
import com.orderhub.repository.ProductRepository;
import com.orderhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedCatalogAndInventory();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("==> Đang khởi tạo tài khoản mặc định...");

            // 1. Tài khoản ADMIN
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Quản Trị Viên Hệ Thống")
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);

            // 2. Tài khoản CUSTOMER mẫu
            User customer = User.builder()
                    .username("customer")
                    .password(passwordEncoder.encode("customer123"))
                    .fullName("Nguyễn Văn Khách")
                    .role(Role.CUSTOMER)
                    .build();
            userRepository.save(customer);

            log.info("==> Khởi tạo tài khoản thành công: admin/admin123 và customer/customer123");
        }
    }

    private void seedCatalogAndInventory() {
        if (categoryRepository.count() == 0) {
            log.info("==> Đang khởi tạo Danh mục, Sản phẩm và Tồn kho ban đầu...");

            // Danh mục 1: Điện Thoại & Tablet
            Category electronics = categoryRepository.save(
                    Category.builder().name("Điện tử & Công nghệ").code("CAT-TECH").build()
            );

            // Danh mục 2: Phụ Kiện
            Category accessories = categoryRepository.save(
                    Category.builder().name("Phụ kiện chính hãng").code("CAT-ACC").build()
            );

            // Sản phẩm 1
            Product p1 = productRepository.save(Product.builder()
                    .sku("TECH-IP15PM")
                    .name("iPhone 15 Pro Max 256GB")
                    .price(new BigDecimal("29990000"))
                    .category(electronics)
                    .build());

            // Tồn kho Sản phẩm 1 (Tổng 20 cái, chưa reserve cái nào)
            inventoryRepository.save(Inventory.builder()
                    .product(p1)
                    .totalQuantity(20)
                    .reservedQuantity(0)
                    .build());

            // Sản phẩm 2
            Product p2 = productRepository.save(Product.builder()
                    .sku("ACC-AIRPODS3")
                    .name("Tai nghe Apple AirPods 3")
                    .price(new BigDecimal("3990000"))
                    .category(accessories)
                    .build());

            // Tồn kho Sản phẩm 2 (Tổng 50 cái)
            inventoryRepository.save(Inventory.builder()
                    .product(p2)
                    .totalQuantity(50)
                    .reservedQuantity(0)
                    .build());

            log.info("==> Khởi tạo thành công: 2 Categories, 2 Products, 2 Inventories!");
        }
    }
}