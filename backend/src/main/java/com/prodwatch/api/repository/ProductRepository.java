package com.prodwatch.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prodwatch.api.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByEcosystem_Id(UUID ecosystemId);

    Optional<Product> findByIdAndEcosystem_Id(UUID id, UUID ecosystemId);

    Optional<Product> findByEcosystem_IdAndSku(UUID ecosystemId, String sku);

    boolean existsByEcosystem_IdAndSku(UUID ecosystemId, String sku);

    boolean existsByEcosystem_IdAndDefaultWarehouse_Id(UUID ecosystemId, UUID warehouseId);

    List<Product> findByEcosystem_IdAndCategoryIgnoreCase(UUID ecosystemId, String category);

    List<Product> findByEcosystem_IdAndNameContainingIgnoreCase(UUID ecosystemId, String name);

    List<Product> findByEcosystem_IdAndCategoryIgnoreCaseAndNameContainingIgnoreCase(
            UUID ecosystemId, String category, String name);
}
