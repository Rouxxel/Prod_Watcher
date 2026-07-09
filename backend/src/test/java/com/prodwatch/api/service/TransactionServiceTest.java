package com.prodwatch.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.prodwatch.api.dto.movement.StockMovementCreate;
import com.prodwatch.api.dto.transaction.CartItemDto;
import com.prodwatch.api.dto.transaction.TransactionCreate;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.StockMovementType;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.error.InsufficientStockException;
import com.prodwatch.api.repository.ProductRepository;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.repository.WarehouseRepository;
import com.prodwatch.api.support.AbstractIntegrationTest;
import com.prodwatch.api.support.TestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionServiceTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private InventoryBalanceService inventoryBalanceService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private Product product;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        TestFixtures.seedUser(profileRepository, userRoleRepository, TestFixtures.CASHIER_ID, AppRole.cashier);
        TestFixtures.seedUser(profileRepository, userRoleRepository, TestFixtures.WORKER_ID, AppRole.warehouse_worker);
        warehouse = warehouseRepository.save(Warehouse.create("POS WH", "Front"));
        product = productRepository.save(Product.create(
                "POS item",
                "POS-001",
                "general",
                new BigDecimal("10.00"),
                warehouse,
                2,
                new String[0]));

        stockMovementService.create(
                new StockMovementCreate(StockMovementType.IN, product.getId(), 10, null, warehouse.getId(), "Test Supplier", null, "seed"),
                TestFixtures.currentUser(TestFixtures.WORKER_ID, AppRole.warehouse_worker));
    }

    @Test
    void checkoutDeductsStockAtomically() {
        TransactionCreate request = checkoutRequest(2, new BigDecimal("20.00"), new BigDecimal("3.20"), new BigDecimal("23.20"));

        transactionService.checkout(request, TestFixtures.currentUser(TestFixtures.CASHIER_ID, AppRole.cashier));

        assertThat(inventoryBalanceService.getStockAtDefaultWarehouse(product.getId())).isEqualTo(8);
    }

    @Test
    void checkoutFailsOnOversell() {
        TransactionCreate request = checkoutRequest(20, new BigDecimal("200.00"), new BigDecimal("32.00"), new BigDecimal("232.00"));

        assertThatThrownBy(() -> transactionService.checkout(
                        request, TestFixtures.currentUser(TestFixtures.CASHIER_ID, AppRole.cashier)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(inventoryBalanceService.getStockAtDefaultWarehouse(product.getId())).isEqualTo(10);
    }

    private TransactionCreate checkoutRequest(int qty, BigDecimal subtotal, BigDecimal tax, BigDecimal total) {
        CartItemDto item = new CartItemDto(
                product.getId(), product.getName(), product.getSku(), qty, product.getPrice());
        return new TransactionCreate(List.of(item), subtotal, tax, total);
    }
}
