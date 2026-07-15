package com.prodwatch.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.Profile;
import com.prodwatch.api.entity.StockMovement;
import com.prodwatch.api.entity.StockMovementType;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.repository.EcosystemRepository;
import com.prodwatch.api.support.TestFixtures;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockMovementRepositoryTest {

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private EcosystemRepository ecosystemRepository;

    @Autowired
    private EntityManager entityManager;

    private Product product;
    private Warehouse source;
    private Warehouse destination;
    private Profile user;

    @BeforeEach
    void setUp() {
        user = TestFixtures.seedUser(
                profileRepository, userRoleRepository, ecosystemRepository, TestFixtures.WORKER_ID, com.prodwatch.api.entity.AppRole.warehouse_worker);
        source = TestFixtures.saveWarehouse(warehouseRepository, ecosystemRepository, "Source WH", "A1");
        destination = TestFixtures.saveWarehouse(warehouseRepository, ecosystemRepository, "Dest WH", "B1");
        product = productRepository.save(
                Product.create("Balance test", "BAL-001", "general", java.math.BigDecimal.TEN, source, 2, new String[0]));
    }

    @Test
    void inventoryBalanceReflectsInOutAndTransfer() {
        stockMovementRepository.save(StockMovement.create(
                StockMovementType.IN, product, 20, null, source, user, "Acme Supply", null, "opening"));
        entityManager.flush();
        assertThat(inventoryBalanceRepository.getStockForProduct(product.getId(), source.getId()))
                .isEqualTo(20);

        stockMovementRepository.save(StockMovement.create(
                StockMovementType.OUT, product, 5, source, null, user, null, "Customer Co", "issue"));
        entityManager.flush();
        assertThat(inventoryBalanceRepository.getStockForProduct(product.getId(), source.getId()))
                .isEqualTo(15);

        stockMovementRepository.save(StockMovement.create(
                StockMovementType.TRANSFER, product, 3, source, destination, user, null, null, "move"));
        entityManager.flush();
        assertThat(inventoryBalanceRepository.getStockForProduct(product.getId(), source.getId()))
                .isEqualTo(12);
        assertThat(inventoryBalanceRepository.getStockForProduct(product.getId(), destination.getId()))
                .isEqualTo(3);
    }

    @Test
    void adjustmentIncreasesAndDecreasesStock() {
        stockMovementRepository.save(StockMovement.create(
                StockMovementType.IN, product, 10, null, source, user, "Seed Co", null, "seed"));

        stockMovementRepository.save(StockMovement.create(
                StockMovementType.ADJUSTMENT, product, 4, null, source, user, null, null, "found extra"));
        entityManager.flush();
        assertThat(inventoryBalanceRepository.getStockForProduct(product.getId(), source.getId()))
                .isEqualTo(14);

        stockMovementRepository.save(StockMovement.create(
                StockMovementType.ADJUSTMENT, product, 2, source, null, user, null, null, "shrinkage"));
        entityManager.flush();
        assertThat(inventoryBalanceRepository.getStockForProduct(product.getId(), source.getId()))
                .isEqualTo(12);
    }
}
