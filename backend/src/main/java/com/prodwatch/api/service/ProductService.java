package com.prodwatch.api.service;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.dto.product.ProductCreate;
import com.prodwatch.api.dto.product.ProductMeta;
import com.prodwatch.api.dto.product.ProductResponse;
import com.prodwatch.api.dto.product.ProductUpdate;
import com.prodwatch.api.cache.RedisCacheKeys;
import com.prodwatch.api.cache.RedisCacheService;
import com.prodwatch.api.cache.RedisCacheTtls;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.ProductRepository;
import com.prodwatch.api.repository.WarehouseRepository;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.TenantContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryBalanceService inventoryBalanceService;
    private final AuditService auditService;
    private final RedisCacheService cacheService;
    private final RedisCacheTtls cacheTtls;

    public ProductService(
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            InventoryBalanceService inventoryBalanceService,
            AuditService auditService,
            RedisCacheService cacheService,
            RedisCacheTtls cacheTtls) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryBalanceService = inventoryBalanceService;
        this.auditService = auditService;
        this.cacheService = cacheService;
        this.cacheTtls = cacheTtls;
    }

    public List<ProductResponse> list(
            String category, String search, Boolean lowStock, UUID stockWarehouseId, CurrentUser user) {
        UUID ecosystemId = TenantContext.requireEcosystemId(user);
        List<Product> products;
        if (category != null && !category.isBlank() && search != null && !search.isBlank()) {
            products = productRepository.findByEcosystem_IdAndCategoryIgnoreCaseAndNameContainingIgnoreCase(
                    ecosystemId, category, search);
        } else if (category != null && !category.isBlank()) {
            products = productRepository.findByEcosystem_IdAndCategoryIgnoreCase(ecosystemId, category);
        } else if (search != null && !search.isBlank()) {
            products = productRepository.findByEcosystem_IdAndNameContainingIgnoreCase(ecosystemId, search);
        } else if (Boolean.TRUE.equals(lowStock)) {
            products = inventoryBalanceService.getLowStockProducts(ecosystemId);
        } else {
            products = productRepository.findAllByEcosystem_Id(ecosystemId);
        }
        return products.stream().map(product -> toResponse(product, stockWarehouseId, ecosystemId)).toList();
    }

    public ProductResponse get(UUID id, CurrentUser user) {
        UUID ecosystemId = TenantContext.requireEcosystemId(user);
        String cacheKey = RedisCacheKeys.productMeta(ecosystemId, id);
        ProductMeta meta = cacheService
                .cacheGet(cacheKey, ProductMeta.class)
                .orElseGet(() -> {
                    Product product = load(id, user);
                    ProductMeta loaded = toMeta(product);
                    cacheService.cacheSet(cacheKey, loaded, cacheTtls.productMeta());
                    return loaded;
                });
        return toResponse(meta, null, ecosystemId);
    }

    @Transactional
    public ProductResponse create(ProductCreate dto, CurrentUser user) {
        UUID ecosystemId = TenantContext.requireEcosystemId(user);
        if (productRepository.existsByEcosystem_IdAndSku(ecosystemId, dto.sku())) {
            throw new BusinessRuleException("SKU already exists");
        }
        Warehouse warehouse = warehouseRepository
                .findByIdAndEcosystem_Id(dto.warehouseId(), ecosystemId)
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
        return toResponse(product, null, ecosystemId);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductUpdate dto, CurrentUser user) {
        UUID ecosystemId = TenantContext.requireEcosystemId(user);
        Product product = load(id, user);

        if (dto.sku() != null && !dto.sku().equals(product.getSku())) {
            if (productRepository.existsByEcosystem_IdAndSku(ecosystemId, dto.sku())) {
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
                    .findByIdAndEcosystem_Id(dto.warehouseId(), ecosystemId)
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
        evictProductMeta(ecosystemId, id);
        return toResponse(product, null, ecosystemId);
    }

    @Transactional
    public void delete(UUID id, CurrentUser user) {
        UUID ecosystemId = TenantContext.requireEcosystemId(user);
        Product product = load(id, user);
        productRepository.delete(product);
        auditService.log(user.getUserId(), "PRODUCT_DELETED", "product", id, product.getSku());
        evictProductMeta(ecosystemId, id);
    }

    Product load(UUID id, CurrentUser user) {
        UUID ecosystemId = TenantContext.requireEcosystemId(user);
        return productRepository
                .findByIdAndEcosystem_Id(id, ecosystemId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void evictProductMeta(UUID ecosystemId, UUID productId) {
        cacheService.cacheDelete(RedisCacheKeys.productMeta(ecosystemId, productId));
    }

    private ProductMeta toMeta(Product product) {
        return new ProductMeta(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getCategory(),
                product.getPrice(),
                product.getDefaultWarehouse().getId(),
                product.getLowStockThreshold(),
                List.of(product.getImages()));
    }

    private ProductResponse toResponse(Product product, UUID stockWarehouseId, UUID ecosystemId) {
        return toResponse(toMeta(product), stockWarehouseId, ecosystemId);
    }

    private ProductResponse toResponse(ProductMeta meta, UUID stockWarehouseId, UUID ecosystemId) {
        int stock = stockWarehouseId != null
                ? inventoryBalanceService.getStock(ecosystemId, meta.id(), stockWarehouseId)
                : inventoryBalanceService.getStockAtDefaultWarehouse(ecosystemId, meta.id());
        return new ProductResponse(
                meta.id(),
                meta.name(),
                meta.sku(),
                meta.category(),
                meta.price(),
                stock,
                meta.defaultWarehouseId(),
                meta.lowStockThreshold(),
                meta.images());
    }
}
