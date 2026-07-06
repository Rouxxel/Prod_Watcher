package com.prodwatch.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prodwatch.api.entity.StockMovement;
import com.prodwatch.api.entity.StockMovementType;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByProduct_IdOrderByCreatedAtDesc(UUID productId);

    List<StockMovement> findByTypeOrderByCreatedAtDesc(StockMovementType type);

    List<StockMovement> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    @Query("""
            SELECT sm FROM StockMovement sm
            WHERE (:productId IS NULL OR sm.product.id = :productId)
              AND (:type IS NULL OR sm.type = :type)
              AND (:from IS NULL OR sm.createdAt >= :from)
              AND (:to IS NULL OR sm.createdAt <= :to)
              AND (
                    :warehouseId IS NULL
                    OR sm.fromWarehouse.id = :warehouseId
                    OR sm.toWarehouse.id = :warehouseId
                  )
            ORDER BY sm.createdAt DESC
            """)
    List<StockMovement> findWithFilters(
            @Param("productId") UUID productId,
            @Param("warehouseId") UUID warehouseId,
            @Param("type") StockMovementType type,
            @Param("from") Instant from,
            @Param("to") Instant to);

    boolean existsByFromWarehouse_IdOrToWarehouse_Id(UUID fromWarehouseId, UUID toWarehouseId);
}
