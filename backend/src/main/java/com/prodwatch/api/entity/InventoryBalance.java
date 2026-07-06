package com.prodwatch.api.entity;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Immutable
@Table(name = "inventory_balances")
public class InventoryBalance {

    @EmbeddedId
    private InventoryBalanceId id;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected InventoryBalance() {}

    public InventoryBalanceId getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }
}
