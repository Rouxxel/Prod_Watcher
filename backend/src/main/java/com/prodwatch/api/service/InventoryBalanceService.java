package com.prodwatch.api.service;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.entity.Product;
import com.prodwatch.api.error.InsufficientStockException;
import com.prodwatch.api.repository.InventoryBalanceRepository;
import com.prodwatch.api.repository.ProductRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;

@Service
public class InventoryBalanceService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public InventoryBalanceService(
            InventoryBalanceRepository inventoryBalanceRepository,
            ProductRepository productRepository,
            EntityManager entityManager) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    public int getStock(UUID productId, UUID warehouseId) {
        entityManager.flush();
        return inventoryBalanceRepository.getStockForProduct(productId, warehouseId);
    }

    public int getStockAtDefaultWarehouse(UUID productId) {
        entityManager.flush();
        return productRepository
                .findById(productId)
                .map(p -> inventoryBalanceRepository.getStockForProduct(
                        productId, p.getDefaultWarehouse().getId()))
                .orElse(0);
    }

    public void assertSufficientStock(UUID productId, UUID warehouseId, int qty) {
        int available = getStock(productId, warehouseId);
        if (available < qty) {
            throw new InsufficientStockException(
                    "Insufficient stock: need " + qty + ", have " + available);
        }
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findAll().stream()
                .filter(p -> getStockAtDefaultWarehouse(p.getId()) <= p.getLowStockThreshold())
                .toList();
    }
}
