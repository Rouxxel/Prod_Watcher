package com.prodwatch.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prodwatch.api.entity.Warehouse;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    List<Warehouse> findAllByEcosystem_Id(UUID ecosystemId);

    Optional<Warehouse> findByIdAndEcosystem_Id(UUID id, UUID ecosystemId);
}
