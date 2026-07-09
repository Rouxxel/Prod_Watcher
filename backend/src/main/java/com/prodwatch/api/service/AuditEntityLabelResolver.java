package com.prodwatch.api.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.prodwatch.api.entity.AuditEntry;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.StockMovement;
import com.prodwatch.api.entity.Transaction;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.repository.ProductRepository;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.StockMovementRepository;
import com.prodwatch.api.repository.TransactionRepository;
import com.prodwatch.api.repository.WarehouseRepository;

import org.springframework.stereotype.Component;

@Component
public class AuditEntityLabelResolver {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProfileRepository profileRepository;
    private final StockMovementRepository stockMovementRepository;
    private final TransactionRepository transactionRepository;

    public AuditEntityLabelResolver(
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            ProfileRepository profileRepository,
            StockMovementRepository stockMovementRepository,
            TransactionRepository transactionRepository) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.profileRepository = profileRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.transactionRepository = transactionRepository;
    }

    public Map<String, String> resolveLabels(List<AuditEntry> entries) {
        if (entries.isEmpty()) {
            return Map.of();
        }

        Map<String, String> labels = new HashMap<>();
        resolveProducts(entityIds(entries, "product"), labels);
        resolveWarehouses(entityIds(entries, "warehouse"), labels);
        resolveUsers(entityIds(entries, "user"), labels);
        resolveMovements(entityIds(entries, "movement"), labels);
        resolveTransactions(entityIds(entries, "transaction"), labels);
        return labels;
    }

    public String resolveLabel(AuditEntry entry, Map<String, String> labels) {
        String label = labels.get(key(entry.getEntity(), entry.getEntityId()));
        if (label != null && !label.isBlank()) {
            return label;
        }
        if (entry.getDetails() != null && !entry.getDetails().isBlank()) {
            return entry.getDetails();
        }
        return shortId(entry.getEntityId());
    }

    private void resolveProducts(Set<UUID> ids, Map<String, String> labels) {
        if (ids.isEmpty()) {
            return;
        }
        for (Product product : productRepository.findAllById(ids)) {
            labels.put(key("product", product.getId()), product.getName());
        }
    }

    private void resolveWarehouses(Set<UUID> ids, Map<String, String> labels) {
        if (ids.isEmpty()) {
            return;
        }
        for (Warehouse warehouse : warehouseRepository.findAllById(ids)) {
            labels.put(key("warehouse", warehouse.getId()), warehouse.getName());
        }
    }

    private void resolveUsers(Set<UUID> ids, Map<String, String> labels) {
        if (ids.isEmpty()) {
            return;
        }
        for (Profile profile : profileRepository.findAllById(ids)) {
            String label = profile.getName();
            if (label == null || label.isBlank()) {
                label = profile.getEmail();
            }
            labels.put(key("user", profile.getId()), label);
        }
    }

    private void resolveMovements(Set<UUID> ids, Map<String, String> labels) {
        if (ids.isEmpty()) {
            return;
        }
        for (StockMovement movement : stockMovementRepository.findAllWithProductByIdIn(ids)) {
            Product product = movement.getProduct();
            String label = product != null ? product.getName() : null;
            if (label != null && !label.isBlank()) {
                labels.put(key("movement", movement.getId()), label);
            }
        }
    }

    private void resolveTransactions(Set<UUID> ids, Map<String, String> labels) {
        if (ids.isEmpty()) {
            return;
        }
        for (Transaction transaction : transactionRepository.findAllById(ids)) {
            labels.put(key("transaction", transaction.getId()), formatTransactionLabel(transaction));
        }
    }

    private static String formatTransactionLabel(Transaction transaction) {
        return "$" + transaction.getTotal().setScale(2).toPlainString();
    }

    private static Set<UUID> entityIds(List<AuditEntry> entries, String entity) {
        Set<UUID> ids = new HashSet<>();
        for (AuditEntry entry : entries) {
            if (entity.equals(entry.getEntity())) {
                ids.add(entry.getEntityId());
            }
        }
        return ids;
    }

    private static String key(String entity, UUID id) {
        return entity + ":" + id;
    }

    private static String shortId(UUID id) {
        String value = id.toString();
        return value.length() > 8 ? value.substring(0, 8) + "…" : value;
    }
}
