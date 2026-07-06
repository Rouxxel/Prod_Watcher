package com.prodwatch.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.prodwatch.api.entity.Transaction;
import com.prodwatch.api.entity.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByCashier_IdOrderByCreatedAtDesc(UUID cashierId);

    List<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (:cashierId IS NULL OR t.cashier.id = :cashierId)
              AND (:status IS NULL OR t.status = :status)
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to IS NULL OR t.createdAt <= :to)
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findWithFilters(
            @Param("cashierId") UUID cashierId,
            @Param("status") TransactionStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
