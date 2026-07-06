package com.prodwatch.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prodwatch.api.entity.Warehouse;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {}
