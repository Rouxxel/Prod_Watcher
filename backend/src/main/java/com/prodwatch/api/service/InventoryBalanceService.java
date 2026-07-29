package com.prodwatch.api.service;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.cache.RedisCacheKeys;
import com.prodwatch.api.cache.RedisCacheService;
import com.prodwatch.api.cache.RedisCacheTtls;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.error.InsufficientStockException;
import com.prodwatch.api.repository.InventoryBalanceRepository;
import com.prodwatch.api.repository.ProductRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class InventoryBalanceService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;
    private final RedisCacheService cacheService;
    private final RedisCacheTtls cacheTtls;

    public InventoryBalanceService(
            InventoryBalanceRepository inventoryBalanceRepository,
            ProductRepository productRepository,
            EntityManager entityManager,
            RedisCacheService cacheService,
            RedisCacheTtls cacheTtls) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
        this.cacheService = cacheService;
        this.cacheTtls = cacheTtls;
    }

    /** Display-only stock with short TTL cache — not for checkout authorization. */
    public int getStock(UUID ecosystemId, UUID productId, UUID warehouseId) {
        String cacheKey = RedisCacheKeys.stockDisplay(ecosystemId, productId, warehouseId);
        return cacheService
                .cacheGet(cacheKey, Integer.class)
                .orElseGet(() -> {
                    int quantity = loadLiveStock(productId, warehouseId);
                    cacheService.cacheSet(cacheKey, quantity, cacheTtls.stockDisplay());
                    return quantity;
                });
    }

    /** Display-only stock at default warehouse with short TTL cache. */
    public int getStockAtDefaultWarehouse(UUID ecosystemId, UUID productId) {
        return productRepository
                .findById(productId)
                .map(p -> getStock(ecosystemId, productId, p.getDefaultWarehouse().getId()))
                .orElse(0);
    }

    /** Authoritative stock read for checkout / oversell checks — never cached. */
    public int getStockForCheckout(UUID productId, UUID warehouseId) {
        flushIfInTransaction();
        return inventoryBalanceRepository.getStockForProduct(productId, warehouseId);
    }

    public void assertSufficientStock(UUID productId, UUID warehouseId, int qty) {
        int available = getStockForCheckout(productId, warehouseId);
        if (available < qty) {
            throw new InsufficientStockException(
                    "Insufficient stock: need " + qty + ", have " + available);
        }
    }

    public List<Product> getLowStockProducts(UUID ecosystemId) {
        return productRepository.findAllByEcosystem_Id(ecosystemId).stream()
                .filter(p -> getStockAtDefaultWarehouse(ecosystemId, p.getId()) <= p.getLowStockThreshold())
                .toList();
    }

    public void evictStockDisplay(UUID ecosystemId, UUID productId, UUID warehouseId) {
        if (warehouseId == null) {
            return;
        }
        cacheService.cacheDelete(RedisCacheKeys.stockDisplay(ecosystemId, productId, warehouseId));
    }

    public void evictStockDisplayForMovement(
            UUID ecosystemId, UUID productId, UUID fromWarehouseId, UUID toWarehouseId) {
        evictStockDisplay(ecosystemId, productId, fromWarehouseId);
        evictStockDisplay(ecosystemId, productId, toWarehouseId);
    }

    private int loadLiveStock(UUID productId, UUID warehouseId) {
        flushIfInTransaction();
        return inventoryBalanceRepository.getStockForProduct(productId, warehouseId);
    }

    private void flushIfInTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            entityManager.flush();
        }
    }
}
