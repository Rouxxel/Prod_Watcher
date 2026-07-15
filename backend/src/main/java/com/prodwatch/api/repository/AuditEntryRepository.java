package com.prodwatch.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prodwatch.api.entity.AuditEntry;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {

    List<AuditEntry> findByEntityOrderByCreatedAtDesc(String entity);

    List<AuditEntry> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<AuditEntry> findByEntityIdOrderByCreatedAtDesc(UUID entityId);

    List<AuditEntry> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    // Date bounds use COALESCE so the timestamp params carry a determinable type on Postgres
    // (a bare ":from IS NULL" triggers "could not determine data type of parameter").
    @Query("""
            SELECT ae FROM AuditEntry ae
            WHERE (:entity IS NULL OR ae.entity = :entity)
              AND (:userId IS NULL OR ae.user.id = :userId)
              AND ae.createdAt >= COALESCE(:from, ae.createdAt)
              AND ae.createdAt <= COALESCE(:to, ae.createdAt)
            ORDER BY ae.createdAt DESC
            """)
    List<AuditEntry> findWithFilters(
            @Param("entity") String entity,
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
