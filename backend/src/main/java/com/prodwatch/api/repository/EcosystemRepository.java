package com.prodwatch.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prodwatch.api.entity.Ecosystem;

public interface EcosystemRepository extends JpaRepository<Ecosystem, UUID> {}
