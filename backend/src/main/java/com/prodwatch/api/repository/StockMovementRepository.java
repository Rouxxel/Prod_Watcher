package com.prodwatch.api.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    Optional<StockMovement> findByIdAndEcosystem_Id(UUID id, UUID ecosystemId);

    @Query("""
            SELECT sm FROM StockMovement sm
            WHERE sm.ecosystem.id = :ecosystemId
              AND (:productId IS NULL OR sm.product.id = :productId)
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
            @Param("ecosystemId") UUID ecosystemId,
            @Param("productId") UUID productId,
            @Param("warehouseId") UUID warehouseId,
            @Param("type") String type,
            @Param("from") Instant from,
            @Param("to") Instant to);

    boolean existsByEcosystem_IdAndFromWarehouse_IdOrEcosystem_IdAndToWarehouse_Id(
            UUID ecosystemId, UUID fromWarehouseId, UUID toWarehouseId);

    @Query("SELECT sm FROM StockMovement sm JOIN FETCH sm.product WHERE sm.id IN :ids")
    List<StockMovement> findAllWithProductByIdIn(@Param("ids") Collection<UUID> ids);
}
