package com.prodwatch.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.prodwatch.api.dto.movement.StockMovementCreate;
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
class StockMovementServiceTest extends AbstractIntegrationTest {

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
    private Warehouse warehouseA;
    private Warehouse warehouseB;

    @BeforeEach
    void setUp() {
        TestFixtures.seedUser(profileRepository, userRoleRepository, TestFixtures.WORKER_ID, AppRole.warehouse_worker);
        warehouseA = warehouseRepository.save(Warehouse.create("WH-A", "Zone A"));
        warehouseB = warehouseRepository.save(Warehouse.create("WH-B", "Zone B"));
        product = productRepository.save(Product.create(
                "Movement test",
                "MOV-001",
                "general",
                new BigDecimal("12.50"),
                warehouseA,
                3,
                new String[0]));
    }

    @Test
    void inIncreasesStock() {
        stockMovementService.create(
                new StockMovementCreate(StockMovementType.IN, product.getId(), 10, null, warehouseA.getId(), null),
                TestFixtures.currentUser(TestFixtures.WORKER_ID, AppRole.warehouse_worker));

        assertThat(inventoryBalanceService.getStock(product.getId(), warehouseA.getId())).isEqualTo(10);
    }

    @Test
    void outDecreasesStock() {
        seedStock(10);

        stockMovementService.create(
                new StockMovementCreate(StockMovementType.OUT, product.getId(), 4, warehouseA.getId(), null, null),
                TestFixtures.currentUser(TestFixtures.WORKER_ID, AppRole.warehouse_worker));

        assertThat(inventoryBalanceService.getStock(product.getId(), warehouseA.getId())).isEqualTo(6);
    }

    @Test
    void outFailsWhenInsufficientStock() {
        seedStock(2);

        assertThatThrownBy(() -> stockMovementService.create(
                        new StockMovementCreate(StockMovementType.OUT, product.getId(), 5, warehouseA.getId(), null, null),
                        TestFixtures.currentUser(TestFixtures.WORKER_ID, AppRole.warehouse_worker)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void transferMovesStockBetweenWarehouses() {
        seedStock(8);

        stockMovementService.create(
                new StockMovementCreate(
                        StockMovementType.TRANSFER,
                        product.getId(),
                        3,
                        warehouseA.getId(),
                        warehouseB.getId(),
                        null),
                TestFixtures.currentUser(TestFixtures.WORKER_ID, AppRole.warehouse_worker));

        assertThat(inventoryBalanceService.getStock(product.getId(), warehouseA.getId())).isEqualTo(5);
        assertThat(inventoryBalanceService.getStock(product.getId(), warehouseB.getId())).isEqualTo(3);
    }

    @Test
    void adjustmentHandlesPositiveAndNegative() {
        seedStock(6);

        stockMovementService.create(
                new StockMovementCreate(StockMovementType.ADJUSTMENT, product.getId(), 2, null, warehouseA.getId(), "found"),
                TestFixtures.currentUser(TestFixtures.WORKER_ID, AppRole.warehouse_worker));
        assertThat(inventoryBalanceService.getStock(product.getId(), warehouseA.getId())).isEqualTo(8);

        stockMovementService.create(
                new StockMovementCreate(StockMovementType.ADJUSTMENT, product.getId(), 3, warehouseA.getId(), null, "loss"),
                TestFixtures.currentUser(TestFixtures.WORKER_ID, AppRole.warehouse_worker));
        assertThat(inventoryBalanceService.getStock(product.getId(), warehouseA.getId())).isEqualTo(5);
    }

    private void seedStock(int qty) {
        stockMovementService.create(
                new StockMovementCreate(StockMovementType.IN, product.getId(), qty, null, warehouseA.getId(), "seed"),
                TestFixtures.currentUser(TestFixtures.WORKER_ID, AppRole.warehouse_worker));
    }
}
