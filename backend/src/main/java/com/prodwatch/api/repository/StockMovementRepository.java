package com.prodwatch.api.repository;

import java.time.Instant;
import java.util.Collection;
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

    // :type is passed as a String (enum name) and the column is cast to string to avoid the
    // Postgres "operator does not exist: enum = varchar" error. Date bounds use COALESCE so the
    // timestamp params carry a determinable type (a bare ":from IS NULL" fails on Postgres).
    @Query("""
            SELECT sm FROM StockMovement sm
            WHERE (:productId IS NULL OR sm.product.id = :productId)
              AND (:type IS NULL OR CAST(sm.type AS string) = :type)
              AND sm.createdAt >= COALESCE(:from, sm.createdAt)
              AND sm.createdAt <= COALESCE(:to, sm.createdAt)
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
            @Param("type") String type,
            @Param("from") Instant from,
            @Param("to") Instant to);

    boolean existsByFromWarehouse_IdOrToWarehouse_Id(UUID fromWarehouseId, UUID toWarehouseId);

    @Query("SELECT sm FROM StockMovement sm JOIN FETCH sm.product WHERE sm.id IN :ids")
    List<StockMovement> findAllWithProductByIdIn(@Param("ids") Collection<UUID> ids);
}
