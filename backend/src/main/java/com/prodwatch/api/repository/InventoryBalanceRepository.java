package com.prodwatch.api.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prodwatch.api.entity.InventoryBalance;
import com.prodwatch.api.entity.InventoryBalanceId;

public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, InventoryBalanceId> {

    List<InventoryBalance> findById_ProductId(UUID productId);

    Optional<InventoryBalance> findById_ProductIdAndId_WarehouseId(UUID productId, UUID warehouseId);

    @Query("""
            SELECT COALESCE(ib.quantity, 0)
            FROM InventoryBalance ib
            WHERE ib.id.productId = :productId AND ib.id.warehouseId = :warehouseId
            """)
    Optional<Integer> findQuantity(
            @Param("productId") UUID productId, @Param("warehouseId") UUID warehouseId);

    default int getStockForProduct(UUID productId, UUID warehouseId) {
        return findQuantity(productId, warehouseId).orElse(0);
    }

    default Map<UUID, Integer> getStockSummaryForProduct(UUID productId) {
        Map<UUID, Integer> summary = new HashMap<>();
        for (InventoryBalance balance : findById_ProductId(productId)) {
            summary.put(balance.getId().getWarehouseId(), balance.getQuantity());
        }
        return summary;
    }
}
