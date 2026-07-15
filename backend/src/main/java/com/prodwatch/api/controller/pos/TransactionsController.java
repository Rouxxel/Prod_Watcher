package com.prodwatch.api.controller.pos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.prodwatch.api.config.RateLimit;
import com.prodwatch.api.dto.transaction.TransactionCreate;
import com.prodwatch.api.dto.transaction.TransactionResponse;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.TransactionStatus;
import com.prodwatch.api.security.CurrentUser;
import com.prodwatch.api.security.RoleChecker;
import com.prodwatch.api.service.TransactionService;

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
        "${config.endpoints.transactions_endpoint.endpoint_prefix}${config.endpoints.transactions_endpoint.endpoint_route}")
@Tag(name = "transactions")
public class TransactionsController {

    private final TransactionService transactionService;

    public TransactionsController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RateLimit("transactions_endpoint")
    @GetMapping
    public List<TransactionResponse> list(
            @RequestParam(required = false) UUID cashierId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin, AppRole.warehouse_manager, AppRole.cashier, AppRole.inspector);
        return transactionService.list(cashierId, status, from, to, user);
    }

    @RateLimit("transactions_endpoint")
    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin, AppRole.warehouse_manager, AppRole.cashier, AppRole.inspector);
        return transactionService.get(id, user);
    }

    @RateLimit("transactions_endpoint")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse checkout(
            @Valid @RequestBody TransactionCreate body, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin, AppRole.cashier);
        return transactionService.checkout(body, user);
    }

    @RateLimit("transactions_endpoint")
    @PostMapping("/{id}/refund")
    public TransactionResponse refund(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin);
        return transactionService.refund(id, user);
    }

    @RateLimit("transactions_endpoint")
    @PostMapping("/{id}/void")
    public TransactionResponse voidTransaction(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser user) {
        RoleChecker.requireRole(user, AppRole.admin);
        return transactionService.voidTransaction(id, user);
    }
}
