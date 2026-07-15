package com.prodwatch.api.controller.inventory;

import java.util.List;
import java.util.UUID;

import com.prodwatch.api.config.RateLimit;
import com.prodwatch.api.dto.warehouse.WarehouseCreate;
import com.prodwatch.api.dto.warehouse.WarehouseResponse;
import com.prodwatch.api.dto.warehouse.WarehouseUpdate;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.RoleChecker;
import com.prodwatch.api.service.WarehouseService;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "${config.endpoints.warehouses_endpoint.endpoint_prefix}${config.endpoints.warehouses_endpoint.endpoint_route}")
@Tag(name = "warehouses")
public class WarehousesController {

    private final WarehouseService warehouseService;

    public WarehousesController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @RateLimit("warehouses_endpoint")
    @GetMapping
    public List<WarehouseResponse> list(@AuthenticationPrincipal CurrentUser user) {
        return warehouseService.list(user);
    }

    @RateLimit("warehouses_endpoint")
    @GetMapping("/{id}")
    public WarehouseResponse get(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        return warehouseService.get(id, user);
    }

    @RateLimit("warehouses_endpoint")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(
            @Valid @RequestBody WarehouseCreate body, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin, AppRole.warehouse_manager);
        return warehouseService.create(body, user);
    }

    @RateLimit("warehouses_endpoint")
    @PatchMapping("/{id}")
    public WarehouseResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody WarehouseUpdate body,
            @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin, AppRole.warehouse_manager);
        return warehouseService.update(id, body, user);
    }

    @RateLimit("warehouses_endpoint")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin, AppRole.warehouse_manager);
        warehouseService.delete(id, user);
    }
}
