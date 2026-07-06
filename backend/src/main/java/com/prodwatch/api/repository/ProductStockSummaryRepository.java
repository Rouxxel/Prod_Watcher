package com.prodwatch.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prodwatch.api.entity.ProductStockSummary;

public interface ProductStockSummaryRepository extends JpaRepository<ProductStockSummary, UUID> {

    default int getDefaultWarehouseStock(UUID productId) {
        return findById(productId).map(ProductStockSummary::getQuantity).orElse(0);
    }
}
