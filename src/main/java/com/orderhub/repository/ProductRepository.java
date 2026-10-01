package com.orderhub.repository;

import com.orderhub.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    // Atomic update: chỉ trừ khi số lượng trong kho >= số lượng cần mua
    // Trả về số dòng bị ảnh hưởng (1 nếu thành công, 0 nếu không đủ hàng)
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :qty " +
            "WHERE p.id = :productId AND p.stockQuantity >= :qty")
    int decreaseStock(@Param("productId") Long productId, @Param("qty") Integer qty);

    // Hoàn kho khi hủy đơn
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :qty " +
            "WHERE p.id = :productId")
    void restoreStock(@Param("productId") Long productId, @Param("qty") Integer qty);
}