package com.prodwatch.api.controller.inventory;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.config.RateLimit;
import com.prodwatch.api.dto.product.ProductCreate;
import com.prodwatch.api.dto.product.ProductResponse;
import com.prodwatch.api.dto.product.ProductUpdate;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.RoleChecker;
import com.prodwatch.api.service.ProductService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "${config.endpoints.products_endpoint.endpoint_prefix}${config.endpoints.products_endpoint.endpoint_route}")
@Tag(name = "products")
public class ProductsController {

    private final ProductService productService;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @RateLimit("products_endpoint")
    @GetMapping
    public List<ProductResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) UUID warehouseId) {
        return productService.list(category, search, lowStock, warehouseId);
    }

    @RateLimit("products_endpoint")
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return productService.get(id);
    }

    @RateLimit("products_endpoint")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @Valid @RequestBody ProductCreate body, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin, AppRole.warehouse_manager, AppRole.warehouse_worker);
        return productService.create(body, user);
    }

    @RateLimit("products_endpoint")
    @PatchMapping("/{id}")
    public ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdate body,
            @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin, AppRole.warehouse_manager, AppRole.warehouse_worker);
        return productService.update(id, body, user);
    }

    @RateLimit("products_endpoint")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin);
        productService.delete(id, user);
    }
}
