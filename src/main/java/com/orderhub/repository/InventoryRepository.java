package com.orderhub.repository;

import com.orderhub.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    // 1. Trừ kho an toàn (Atomic Reserve): Chỉ tăng reserved khi hàng khả dụng còn đủ
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity + :qty " +
            "WHERE i.product.id = :productId AND (i.totalQuantity - i.reservedQuantity) >= :qty")
    int reserveStock(@Param("productId") Long productId, @Param("qty") Integer qty);

    // 2. Hoàn lại kho khi đơn hàng bị HỦY (Release reservation)
    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity - :qty " +
            "WHERE i.product.id = :productId AND i.reservedQuantity >= :qty")
    int releaseStock(@Param("productId") Long productId, @Param("qty") Integer qty);

    // 3. Khách đã nhận hàng thành công (SHIPPED) -> trừ đứt cả total lẫn reserved
    @Modifying
    @Query("UPDATE Inventory i SET i.totalQuantity = i.totalQuantity - :qty, " +
            "i.reservedQuantity = i.reservedQuantity - :qty " +
            "WHERE i.product.id = :productId AND i.reservedQuantity >= :qty")
    int deductStockOnShipment(@Param("productId") Long productId, @Param("qty") Integer qty);
}