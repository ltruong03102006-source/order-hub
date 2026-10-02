package com.orderhub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Version
    private Long version;

    // Helper method tính lượng tồn kho thực tế có thể bán
    public Integer getAvailableQuantity() {
        return totalQuantity - reservedQuantity;
    }
}