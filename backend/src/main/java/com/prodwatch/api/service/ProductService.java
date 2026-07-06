package com.prodwatch.api.service;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.dto.product.ProductCreate;
import com.prodwatch.api.dto.product.ProductResponse;
import com.prodwatch.api.dto.product.ProductUpdate;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.ProductRepository;
import com.prodwatch.api.repository.WarehouseRepository;
import com.prodwatch.api.security.CurrentUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryBalanceService inventoryBalanceService;
    private final AuditService auditService;

    public ProductService(
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            InventoryBalanceService inventoryBalanceService,
            AuditService auditService) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryBalanceService = inventoryBalanceService;
        this.auditService = auditService;
    }

    public List<ProductResponse> list(String category, String search, Boolean lowStock) {
        List<Product> products;
        if (category != null && !category.isBlank() && search != null && !search.isBlank()) {
            products = productRepository.findByCategoryIgnoreCaseAndNameContainingIgnoreCase(category, search);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByCategoryIgnoreCase(category);
        } else if (search != null && !search.isBlank()) {
            products = productRepository.findByNameContainingIgnoreCase(search);
        } else if (Boolean.TRUE.equals(lowStock)) {
            products = inventoryBalanceService.getLowStockProducts();
        } else {
            products = productRepository.findAll();
        }
        return products.stream().map(this::toResponse).toList();
    }

    public ProductResponse get(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public ProductResponse create(ProductCreate dto, CurrentUser user) {
        if (productRepository.existsBySku(dto.sku())) {
            throw new BusinessRuleException("SKU already exists");
        }
        Warehouse warehouse = warehouseRepository
                .findById(dto.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        Product product = productRepository.save(Product.create(
                dto.name(),
                dto.sku(),
                dto.category(),
                dto.price(),
                warehouse,
                dto.lowStockThreshold(),
                dto.images().toArray(String[]::new)));

        auditService.log(user.getUserId(), "PRODUCT_CREATED", "product", product.getId(), product.getSku());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductUpdate dto, CurrentUser user) {
        Product product = load(id);

        if (dto.sku() != null && !dto.sku().equals(product.getSku())) {
            if (productRepository.existsBySku(dto.sku())) {
                throw new BusinessRuleException("SKU already exists");
            }
            product.setSku(dto.sku());
        }
        if (dto.name() != null) {
            product.setName(dto.name());
        }
        if (dto.category() != null) {
            product.setCategory(dto.category());
        }
        if (dto.price() != null) {
            product.setPrice(dto.price());
        }
        if (dto.warehouseId() != null) {
            Warehouse warehouse = warehouseRepository
                    .findById(dto.warehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
            product.setDefaultWarehouse(warehouse);
        }
        if (dto.lowStockThreshold() != null) {
            product.setLowStockThreshold(dto.lowStockThreshold());
        }
        if (dto.images() != null) {
            product.setImages(dto.images().toArray(String[]::new));
        }

        product = productRepository.save(product);
        auditService.log(user.getUserId(), "PRODUCT_UPDATED", "product", id, product.getSku());
        return toResponse(product);
    }

    @Transactional
    public void delete(UUID id, CurrentUser user) {
        Product product = load(id);
        productRepository.delete(product);
        auditService.log(user.getUserId(), "PRODUCT_DELETED", "product", id, product.getSku());
    }

    private Product load(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private ProductResponse toResponse(Product product) {
        int stock = inventoryBalanceService.getStockAtDefaultWarehouse(product.getId());
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCategory(),
                product.getPrice(),
                stock,
                product.getDefaultWarehouse().getId(),
                product.getLowStockThreshold(),
                List.of(product.getImages()));
    }
}
