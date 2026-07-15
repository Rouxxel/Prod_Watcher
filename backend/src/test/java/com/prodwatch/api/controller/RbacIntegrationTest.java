package com.prodwatch.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodwatch.api.dto.movement.StockMovementCreate;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.StockMovementType;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.repository.EcosystemRepository;
import com.prodwatch.api.repository.ProductRepository;
import com.prodwatch.api.repository.ProfileRepository;
import com.prodwatch.api.repository.UserRoleRepository;
import com.prodwatch.api.repository.WarehouseRepository;
import com.prodwatch.api.support.AbstractIntegrationTest;
import com.prodwatch.api.support.TestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RbacIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private UUID productId;
    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        TestFixtures.seedUser(profileRepository, userRoleRepository, ecosystemRepository, TestFixtures.ADMIN_ID, AppRole.admin);
        TestFixtures.seedUser(profileRepository, userRoleRepository, ecosystemRepository, TestFixtures.WORKER_ID, AppRole.warehouse_worker);
        TestFixtures.seedUser(profileRepository, userRoleRepository, ecosystemRepository, TestFixtures.CASHIER_ID, AppRole.cashier);

        Warehouse warehouse = TestFixtures.saveWarehouse(warehouseRepository, ecosystemRepository, "RBAC WH", "Shelf 1");
        warehouseId = warehouse.getId();
        Product product = productRepository.save(Product.create(
                "RBAC product",
                "RBAC-001",
                "general",
                new BigDecimal("5.00"),
                warehouse,
                1,
                new String[0]));
        productId = product.getId();
    }

    @Test
    void warehouseWorkerCannotDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.ADMIN_ID)))
                .andExpect(status().isNoContent());
    }

    @Test
    void cashierCannotCreateStockMovement() throws Exception {
        StockMovementCreate body = new StockMovementCreate(
                StockMovementType.IN, productId, 1, null, warehouseId, "Test Supplier", null, null);

        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.CASHIER_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Insufficient permissions"));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", productId)).andExpect(status().isUnauthorized());
    }
}
