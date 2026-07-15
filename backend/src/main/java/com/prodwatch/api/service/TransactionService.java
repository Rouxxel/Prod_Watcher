package com.prodwatch.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.prodwatch.api.dto.transaction.CartItemDto;
import com.prodwatch.api.dto.transaction.TransactionCreate;
import com.prodwatch.api.dto.transaction.TransactionResponse;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.StockMovementType;
import com.prodwatch.api.entity.Transaction;
import com.prodwatch.api.entity.TransactionLineItem;
import com.prodwatch.api.entity.TransactionStatus;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.error.BusinessRuleException;
import com.prodwatch.api.error.ResourceNotFoundException;
import com.prodwatch.api.repository.ProductRepository;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.StockMovementRepository;
import com.prodwatch.api.repository.TransactionRepository;
import com.prodwatch.api.repository.WarehouseRepository;
import com.prodwatch.api.security.CurrentUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final int MONEY_SCALE = 2;

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final ProfileRepository profileRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryBalanceService inventoryBalanceService;
    private final AuditService auditService;
    private final BigDecimal taxRate;
    private final UUID posWarehouseId;

    public TransactionService(
            TransactionRepository transactionRepository,
            ProductRepository productRepository,
            ProfileRepository profileRepository,
            StockMovementRepository stockMovementRepository,
            WarehouseRepository warehouseRepository,
            InventoryBalanceService inventoryBalanceService,
            AuditService auditService,
            @Value("${prodwatch.pos.tax-rate:0.16}") BigDecimal taxRate,
            @Value("${prodwatch.pos.warehouse-id:#{null}}") UUID posWarehouseId) {
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
        this.profileRepository = profileRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryBalanceService = inventoryBalanceService;
        this.auditService = auditService;
        this.taxRate = taxRate;
        this.posWarehouseId = posWarehouseId;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(
            UUID cashierId, TransactionStatus status, Instant from, Instant to) {
        return transactionRepository
                .findWithFilters(cashierId, status == null ? null : status.name(), from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public TransactionResponse checkout(TransactionCreate dto, CurrentUser cashier) {
        Profile cashierProfile = profileRepository.getReferenceById(cashier.getUserId());
        Warehouse posWarehouse = resolvePosWarehouse();

        List<ValidatedLine> lines = validateAndResolveLines(dto.items(), posWarehouse);

        BigDecimal subtotal = lines.stream()
                .map(ValidatedLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(taxRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        assertAmountMatches("subtotal", dto.subtotal(), subtotal);
        assertAmountMatches("tax", dto.tax(), tax);
        assertAmountMatches("total", dto.total(), total);

        List<TransactionLineItem> lineItems = lines.stream()
                .map(ValidatedLine::toLineItem)
                .toList();

        Transaction transaction =
                transactionRepository.save(Transaction.create(lineItems, subtotal, tax, total, cashierProfile));

        for (ValidatedLine line : lines) {
            stockMovementRepository.save(com.prodwatch.api.entity.StockMovement.create(
                    StockMovementType.OUT,
                    line.product(),
                    line.item().qty(),
                    line.warehouse(),
                    null,
                    cashierProfile,
                    null,
                    "POS customer",
                    "POS sale " + transaction.getId()));
        }

        auditService.log(
                cashier.getUserId(),
                "TRANSACTION_COMPLETED",
                "transaction",
                transaction.getId(),
                null);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse refund(UUID id, CurrentUser user) {
        return reverse(id, user, TransactionStatus.refunded, "TRANSACTION_REFUNDED");
    }

    @Transactional
    public TransactionResponse voidTransaction(UUID id, CurrentUser user) {
        return reverse(id, user, TransactionStatus.void_, "TRANSACTION_VOIDED");
    }

    private TransactionResponse reverse(
            UUID id, CurrentUser user, TransactionStatus newStatus, String auditAction) {
        Transaction transaction = load(id);
        if (transaction.getStatus() != TransactionStatus.completed) {
            throw new BusinessRuleException("Only completed transactions can be reversed");
        }

        Profile actor = profileRepository.getReferenceById(user.getUserId());
        Warehouse posWarehouse = resolvePosWarehouse();

        for (TransactionLineItem item : transaction.getItems()) {
            Product product = productRepository
                    .findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            Warehouse warehouse = item.warehouseId() != null
                    ? warehouseRepository
                            .findById(item.warehouseId())
                            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"))
                    : warehouseForProduct(product, posWarehouse);

            stockMovementRepository.save(com.prodwatch.api.entity.StockMovement.create(
                    StockMovementType.IN,
                    product,
                    item.qty(),
                    null,
                    warehouse,
                    actor,
                    "POS return",
                    null,
                    newStatus + " " + transaction.getId()));
        }

        transaction.setStatus(newStatus);
        auditService.log(user.getUserId(), auditAction, "transaction", transaction.getId(), null);
        return toResponse(transaction);
    }

    private List<ValidatedLine> validateAndResolveLines(List<CartItemDto> items, Warehouse posWarehouse) {
        List<ValidatedLine> lines = new ArrayList<>();
        for (CartItemDto item : items) {
            Product product = productRepository
                    .findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.productId()));
            if (!product.getSku().equals(item.sku())) {
                throw new BusinessRuleException("SKU mismatch for product " + item.productId());
            }
            if (product.getPrice().compareTo(item.unitPrice()) != 0) {
                throw new BusinessRuleException("Price mismatch for product " + product.getSku());
            }

            Warehouse warehouse = resolveWarehouse(item, product, posWarehouse);
            inventoryBalanceService.assertSufficientStock(product.getId(), warehouse.getId(), item.qty());

            BigDecimal lineTotal = item.unitPrice()
                    .multiply(BigDecimal.valueOf(item.qty()))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            lines.add(new ValidatedLine(item, product, warehouse, lineTotal));
        }
        return lines;
    }

    private Warehouse resolveWarehouse(CartItemDto item, Product product, Warehouse posWarehouse) {
        if (item.warehouseId() != null) {
            return warehouseRepository
                    .findById(item.warehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        }
        return warehouseForProduct(product, posWarehouse);
    }

    private Warehouse warehouseForProduct(Product product, Warehouse posWarehouse) {
        if (posWarehouse != null) {
            return posWarehouse;
        }
        return product.getDefaultWarehouse();
    }

    private Warehouse resolvePosWarehouse() {
        if (posWarehouseId == null) {
            return null;
        }
        return warehouseRepository
                .findById(posWarehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("POS warehouse not found"));
    }

    private void assertAmountMatches(String field, BigDecimal actual, BigDecimal expected) {
        if (actual.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                .compareTo(expected) != 0) {
            throw new BusinessRuleException("Invalid " + field + ": expected " + expected + ", got " + actual);
        }
    }

    private Transaction load(UUID id) {
        return transactionRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private TransactionResponse toResponse(Transaction transaction) {
        List<CartItemDto> items = transaction.getItems().stream()
                .map(i -> new CartItemDto(
                        i.productId(), i.name(), i.sku(), i.qty(), i.unitPrice(), i.warehouseId()))
                .toList();
        return new TransactionResponse(
                transaction.getId(),
                items,
                transaction.getSubtotal(),
                transaction.getTax(),
                transaction.getTotal(),
                transaction.getCashier().getId(),
                transaction.getCashier().getName(),
                transaction.getStatus(),
                transaction.getCreatedAt());
    }

    private record ValidatedLine(CartItemDto item, Product product, Warehouse warehouse, BigDecimal lineTotal) {
        TransactionLineItem toLineItem() {
            return new TransactionLineItem(
                    item.productId(),
                    product.getName(),
                    product.getSku(),
                    item.qty(),
                    item.unitPrice(),
                    warehouse.getId());
        }
    }
}
