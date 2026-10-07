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
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        migrateCustomerRole();
        seedUsers();
        seedCatalogAndInventory();
    }

    private void migrateCustomerRole() {
        List<RoleCheckConstraint> roleConstraints = jdbcTemplate.query(
                "SELECT conname, pg_get_constraintdef(oid) AS definition " +
                        "FROM pg_constraint " +
                        "WHERE conrelid = 'users'::regclass AND contype = 'c' " +
                        "AND pg_get_constraintdef(oid) ILIKE '%role%'",
                (resultSet, rowNumber) -> new RoleCheckConstraint(
                        resultSet.getString("conname"),
                        resultSet.getString("definition")
                )
        );

        boolean hasLegacyConstraint = roleConstraints.stream()
                .anyMatch(constraint -> {
                    String definition = constraint.definition().toUpperCase(Locale.ROOT);
                    return definition.contains("'CUSTOMER'") || !definition.contains("'STAFF'");
                });

        if (hasLegacyConstraint) {
            for (RoleCheckConstraint constraint : roleConstraints) {
                String quotedName = "\"" + constraint.name().replace("\"", "\"\"") + "\"";
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT " + quotedName);
            }
        }

        int migratedUsers = jdbcTemplate.update(
                "UPDATE users SET role = 'STAFF' WHERE role = 'CUSTOMER'"
        );

        if (hasLegacyConstraint) {
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'STAFF'))"
            );
        }
        if (migratedUsers > 0) {
            log.info("Migrated {} legacy CUSTOMER accounts to STAFF.", migratedUsers);
        }
    }

    private record RoleCheckConstraint(String name, String definition) {
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("==> Đang khởi tạo tài khoản mặc định...");

            // 1. Tài khoản ADMIN
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Quản Trị Viên Hệ Thống")
                    .email("admin@orderhub.com")
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);

            // 2. Tài khoản nhân viên mẫu
            User staff = User.builder()
                    .username("staff")
                    .password(passwordEncoder.encode("staff123"))
                    .fullName("Nhân viên OrderHub")
                    .email("staff@orderhub.com")
                    .role(Role.STAFF)
                    .build();
            userRepository.save(staff);

            log.info("==> Khởi tạo tài khoản thành công: admin/admin123 và staff/staff123");
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