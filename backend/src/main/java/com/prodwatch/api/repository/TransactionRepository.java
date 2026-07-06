package com.prodwatch.api.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.prodwatch.api.entity.Transaction;
import com.prodwatch.api.entity.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByCashier_IdOrderByCreatedAtDesc(UUID cashierId);

    List<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    List<Transaction> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
