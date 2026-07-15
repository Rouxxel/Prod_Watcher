package com.prodwatch.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prodwatch.api.entity.Transaction;
import com.prodwatch.api.entity.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByCashier_IdOrderByCreatedAtDesc(UUID cashierId);

    boolean existsByCashier_Id(UUID cashierId);

    boolean existsByEcosystem_IdAndCashier_Id(UUID ecosystemId, UUID cashierId);

    List<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    Optional<Transaction> findByIdAndEcosystem_Id(UUID id, UUID ecosystemId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.ecosystem.id = :ecosystemId
              AND (:cashierId IS NULL OR t.cashier.id = :cashierId)
              AND (:status IS NULL OR CAST(t.status AS string) = :status)
              AND t.createdAt >= COALESCE(:from, t.createdAt)
              AND t.createdAt <= COALESCE(:to, t.createdAt)
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findWithFilters(
            @Param("ecosystemId") UUID ecosystemId,
            @Param("cashierId") UUID cashierId,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
