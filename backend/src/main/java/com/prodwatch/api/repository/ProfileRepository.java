package com.prodwatch.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prodwatch.api.entity.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Profile> findAllByEcosystem_Id(UUID ecosystemId);

    Optional<Profile> findByIdAndEcosystem_Id(UUID id, UUID ecosystemId);

    @Query("SELECT p FROM Profile p LEFT JOIN FETCH p.ecosystem WHERE p.id = :id")
    Optional<Profile> findByIdWithEcosystem(@Param("id") UUID id);

    @Query("SELECT p FROM Profile p LEFT JOIN FETCH p.ecosystem WHERE p.ecosystem.id = :ecosystemId")
    List<Profile> findAllByEcosystem_IdWithEcosystem(@Param("ecosystemId") UUID ecosystemId);

    @Query("""
            SELECT p FROM Profile p
            LEFT JOIN FETCH p.ecosystem
            WHERE p.id = :id AND p.ecosystem.id = :ecosystemId
            """)
    Optional<Profile> findByIdAndEcosystem_IdWithEcosystem(
            @Param("id") UUID id, @Param("ecosystemId") UUID ecosystemId);
}
