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

    // :status is passed as a String (DB enum label) and the column is cast to string to avoid the
    // Postgres "operator does not exist: transaction_status = varchar" error. Date bounds use
    // COALESCE so the timestamp params carry a determinable type (a bare ":from IS NULL" fails).
    @Query("""
            SELECT t FROM Transaction t
            WHERE (:cashierId IS NULL OR t.cashier.id = :cashierId)
              AND (:status IS NULL OR CAST(t.status AS string) = :status)
              AND t.createdAt >= COALESCE(:from, t.createdAt)
              AND t.createdAt <= COALESCE(:to, t.createdAt)
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findWithFilters(
            @Param("cashierId") UUID cashierId,
            @Param("status") String status,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
