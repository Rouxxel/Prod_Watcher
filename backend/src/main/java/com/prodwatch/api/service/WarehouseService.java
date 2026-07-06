package com.prodwatch.api.service;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.dto.warehouse.WarehouseCreate;
import com.prodwatch.api.dto.warehouse.WarehouseResponse;
import com.prodwatch.api.dto.warehouse.WarehouseUpdate;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.InventoryBalanceRepository;
import com.prodwatch.api.repository.ProductRepository;
import com.prodwatch.api.repository.StockMovementRepository;
import com.prodwatch.api.repository.WarehouseRepository;
import com.prodwatch.api.security.CurrentUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final AuditService auditService;

    public WarehouseService(
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            StockMovementRepository stockMovementRepository,
            InventoryBalanceRepository inventoryBalanceRepository,
            AuditService auditService) {
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.auditService = auditService;
    }

    public List<WarehouseResponse> list() {
        return warehouseRepository.findAll().stream().map(this::toResponse).toList();
    }

    public WarehouseResponse get(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public WarehouseResponse create(WarehouseCreate dto, CurrentUser user) {
        Warehouse warehouse = warehouseRepository.save(Warehouse.create(dto.name(), dto.location()));
        auditService.log(user.getUserId(), "WAREHOUSE_CREATED", "warehouse", warehouse.getId(), warehouse.getName());
        return toResponse(warehouse);
    }

    @Transactional
    public WarehouseResponse update(UUID id, WarehouseUpdate dto, CurrentUser user) {
        Warehouse warehouse = load(id);
        if (dto.name() != null) {
            warehouse.setName(dto.name());
        }
        if (dto.location() != null) {
            warehouse.setLocation(dto.location());
        }
        warehouse = warehouseRepository.save(warehouse);
        auditService.log(user.getUserId(), "WAREHOUSE_UPDATED", "warehouse", id, warehouse.getName());
        return toResponse(warehouse);
    }

    @Transactional
    public void delete(UUID id, CurrentUser user) {
        Warehouse warehouse = load(id);
        if (productRepository.existsByDefaultWarehouse_Id(id)) {
            throw new BusinessRuleException("Cannot delete warehouse assigned as default on products");
        }
        if (stockMovementRepository.existsByFromWarehouse_IdOrToWarehouse_Id(id, id)) {
            throw new BusinessRuleException("Cannot delete warehouse with stock movement history");
        }
        if (inventoryBalanceRepository.findById_WarehouseId(id).stream()
                .anyMatch(b -> b.getQuantity() > 0)) {
            throw new BusinessRuleException("Cannot delete warehouse with remaining stock");
        }
        warehouseRepository.delete(warehouse);
        auditService.log(user.getUserId(), "WAREHOUSE_DELETED", "warehouse", id, warehouse.getName());
    }

    private Warehouse load(UUID id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getId(), warehouse.getName(), warehouse.getLocation());
    }
}
