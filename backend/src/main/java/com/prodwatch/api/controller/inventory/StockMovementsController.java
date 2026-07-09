package com.prodwatch.api.controller.inventory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.prodwatch.api.config.RateLimit;
import com.prodwatch.api.dto.movement.StockMovementCreate;
import com.prodwatch.api.dto.movement.StockMovementResponse;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.StockMovementType;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.RoleChecker;
import com.prodwatch.api.service.StockMovementService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "${config.endpoints.movements_endpoint.endpoint_prefix}${config.endpoints.movements_endpoint.endpoint_route}")
@Tag(name = "stock-movements")
public class StockMovementsController {

    private final StockMovementService stockMovementService;

    public StockMovementsController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @RateLimit("movements_endpoint")
    @GetMapping
    public List<StockMovementResponse> list(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) StockMovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return stockMovementService.list(productId, warehouseId, type, from, to);
    }

    @RateLimit("movements_endpoint")
    @GetMapping("/{id}")
    public StockMovementResponse get(@PathVariable UUID id) {
        return stockMovementService.get(id);
    }

    @RateLimit("movements_endpoint")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse create(
            @Valid @RequestBody StockMovementCreate body, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(
                user, AppRole.admin, AppRole.warehouse_manager, AppRole.warehouse_worker);
        return stockMovementService.create(body, user);
    }
}
