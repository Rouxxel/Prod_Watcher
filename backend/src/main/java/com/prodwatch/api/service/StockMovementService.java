package com.prodwatch.api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.prodwatch.api.dto.movement.StockMovementCreate;
import com.prodwatch.api.dto.movement.StockMovementResponse;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.StockMovement;
import com.prodwatch.api.entity.StockMovementType;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.ProductRepository;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.StockMovementRepository;
import com.prodwatch.api.repository.WarehouseRepository;
import com.prodwatch.api.security.CurrentUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProfileRepository profileRepository;
    private final InventoryBalanceService inventoryBalanceService;
    private final AuditService auditService;

    public StockMovementService(
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            ProfileRepository profileRepository,
            InventoryBalanceService inventoryBalanceService,
            AuditService auditService) {
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.profileRepository = profileRepository;
        this.inventoryBalanceService = inventoryBalanceService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponse> list(
            UUID productId, UUID warehouseId, StockMovementType type, Instant from, Instant to) {
        return stockMovementRepository
                .findWithFilters(productId, warehouseId, type, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StockMovementResponse get(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public StockMovementResponse create(StockMovementCreate dto, CurrentUser user) {
        Product product = productRepository
                .findById(dto.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Profile profile = profileRepository.getReferenceById(user.getUserId());

        Warehouse fromWarehouse = null;
        Warehouse toWarehouse = null;

        switch (dto.type()) {
            case IN -> {
                if (dto.toWarehouseId() == null) {
                    throw new BusinessRuleException("IN movement requires toWarehouseId");
                }
                toWarehouse = loadWarehouse(dto.toWarehouseId());
            }
            case OUT -> {
                if (dto.fromWarehouseId() == null) {
                    throw new BusinessRuleException("OUT movement requires fromWarehouseId");
                }
                fromWarehouse = loadWarehouse(dto.fromWarehouseId());
                inventoryBalanceService.assertSufficientStock(product.getId(), fromWarehouse.getId(), dto.qty());
            }
            case TRANSFER -> {
                if (dto.fromWarehouseId() == null || dto.toWarehouseId() == null) {
                    throw new BusinessRuleException("TRANSFER requires fromWarehouseId and toWarehouseId");
                }
                if (dto.fromWarehouseId().equals(dto.toWarehouseId())) {
                    throw new BusinessRuleException("TRANSFER warehouses must differ");
                }
                fromWarehouse = loadWarehouse(dto.fromWarehouseId());
                toWarehouse = loadWarehouse(dto.toWarehouseId());
                inventoryBalanceService.assertSufficientStock(product.getId(), fromWarehouse.getId(), dto.qty());
            }
            case ADJUSTMENT -> {
                if (dto.fromWarehouseId() != null && dto.toWarehouseId() != null) {
                    throw new BusinessRuleException("ADJUSTMENT requires only one warehouse");
                }
                if (dto.fromWarehouseId() == null && dto.toWarehouseId() == null) {
                    throw new BusinessRuleException("ADJUSTMENT requires fromWarehouseId or toWarehouseId");
                }
                if (dto.fromWarehouseId() != null) {
                    fromWarehouse = loadWarehouse(dto.fromWarehouseId());
                    inventoryBalanceService.assertSufficientStock(
                            product.getId(), fromWarehouse.getId(), dto.qty());
                } else {
                    toWarehouse = loadWarehouse(dto.toWarehouseId());
                }
            }
        }

        StockMovement movement = stockMovementRepository.save(StockMovement.create(
                dto.type(), product, dto.qty(), fromWarehouse, toWarehouse, profile, dto.note()));

        auditService.log(user.getUserId(), auditAction(dto.type()), "movement", movement.getId(), product.getSku());
        return toResponse(movement);
    }

    private StockMovement load(UUID id) {
        return stockMovementRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock movement not found"));
    }

    private Warehouse loadWarehouse(UUID id) {
        return warehouseRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
    }

    private static String auditAction(StockMovementType type) {
        return switch (type) {
            case IN -> "STOCK_RECEIVED";
            case OUT -> "STOCK_ISSUED";
            case TRANSFER -> "STOCK_TRANSFERRED";
            case ADJUSTMENT -> "STOCK_ADJUSTED";
        };
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getType(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getQty(),
                movement.getFromWarehouse() != null ? movement.getFromWarehouse().getId() : null,
                movement.getToWarehouse() != null ? movement.getToWarehouse().getId() : null,
                movement.getUser().getId(),
                movement.getUser().getName(),
                movement.getCreatedAt(),
                movement.getNote());
    }
}
