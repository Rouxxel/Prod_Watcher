package com.prodwatch.api.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", nullable = false, columnDefinition = "jsonb")
    private List<TransactionLineItem> items = new ArrayList<>();

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal tax;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private Profile cashier;

    @Convert(converter = TransactionStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "transaction_status")
    private TransactionStatus status = TransactionStatus.completed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transaction() {}

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (items == null) {
            items = new ArrayList<>();
        }
    }

    public UUID getId() {
        return id;
    }

    public List<TransactionLineItem> getItems() {
        return items;
    }

    public void setItems(List<TransactionLineItem> items) {
        this.items = items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Profile getCashier() {
        return cashier;
    }

    public void setCashier(Profile cashier) {
        this.cashier = cashier;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static Transaction create(
            List<TransactionLineItem> items,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            Profile cashier) {
        Transaction transaction = new Transaction();
        transaction.items = new ArrayList<>(items);
        transaction.subtotal = subtotal;
        transaction.tax = tax;
        transaction.total = total;
        transaction.cashier = cashier;
        transaction.status = TransactionStatus.completed;
        return transaction;
    }
}
