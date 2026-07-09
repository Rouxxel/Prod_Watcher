package com.prodwatch.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodwatch.api.dto.movement.StockMovementCreate;
import com.prodwatch.api.dto.product.ProductCreate;
import com.prodwatch.api.dto.transaction.CartItemDto;
import com.prodwatch.api.dto.transaction.TransactionCreate;
import com.prodwatch.api.dto.warehouse.WarehouseCreate;
import com.prodwatch.api.dto.warehouse.WarehouseUpdate;
import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.StockMovementType;
import com.prodwatch.api.entity.Warehouse;
import com.prodwatch.api.repository.AuditEntryRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        TestFixtures.seedUser(profileRepository, userRoleRepository, TestFixtures.ADMIN_ID, AppRole.admin);
        TestFixtures.seedUser(profileRepository, userRoleRepository, TestFixtures.WORKER_ID, AppRole.warehouse_worker);
        TestFixtures.seedUser(profileRepository, userRoleRepository, TestFixtures.CASHIER_ID, AppRole.cashier);
        warehouseId = warehouseRepository.save(Warehouse.create("Main WH", "Floor 1")).getId();
    }

    @Test
    void validJwtIsAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/products").header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID)))
                .andExpect(status().isOk());
    }

    @Test
    void inactiveUserJwtIsRejected() throws Exception {
        UUID inactiveId = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
        profileRepository.save(com.prodwatch.api.entity.Profile.create(
                inactiveId, "inactive@test.local", "Inactive User", false));
        userRoleRepository.save(new com.prodwatch.api.entity.UserRole(
                profileRepository.findById(inactiveId).orElseThrow(), AppRole.cashier));

        mockMvc.perform(get("/api/v1/products").header("Authorization", TestFixtures.bearerHeader(inactiveId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header("Origin", "http://localhost:8080")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"));
    }

    @Test
    void productCrudIncludesComputedStock() throws Exception {
        ProductCreate create = new ProductCreate(
                "Test Mug",
                "MUG-001",
                "Kitchen",
                new BigDecimal("12.00"),
                warehouseId,
                2,
                List.of());

        MvcResult created = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stock").value(0))
                .andReturn();

        UUID productId = UUID.fromString(
                objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

        seedStock(productId, 15);

        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(15));

        mockMvc.perform(patch("/api/v1/products/{id}", productId)
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Updated Mug\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Mug"));
    }

    @Test
    void warehouseCrudWorks() throws Exception {
        WarehouseCreate create = new WarehouseCreate("East Hub", "Building 2");

        MvcResult created = mockMvc.perform(post("/api/v1/warehouses")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.ADMIN_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID id = UUID.fromString(
                objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/warehouses/{id}", id)
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("East Hub"));

        mockMvc.perform(patch("/api/v1/warehouses/{id}", id)
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.ADMIN_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WarehouseUpdate("East Hub Updated", "B2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("B2"));
    }

    @Test
    void outMovementRejectedWhenInsufficientStock() throws Exception {
        Product product = productRepository.save(Product.create(
                "Low stock", "LOW-001", "general", BigDecimal.TEN, warehouseRepository.findById(warehouseId).orElseThrow(), 1, new String[0]));

        StockMovementCreate out = new StockMovementCreate(
                StockMovementType.OUT, product.getId(), 5, warehouseId, null, null, "Test Customer", null);

        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(out)))
                .andExpect(status().isConflict());
    }

    @Test
    void transferValidatesSourceWarehouseStock() throws Exception {
        Product product = productRepository.save(Product.create(
                "Transfer item",
                "TRF-001",
                "general",
                BigDecimal.TEN,
                warehouseRepository.findById(warehouseId).orElseThrow(),
                1,
                new String[0]));
        UUID destId = warehouseRepository.save(Warehouse.create("Dest", "D1")).getId();

        seedStock(product.getId(), 3);

        StockMovementCreate transfer = new StockMovementCreate(
                StockMovementType.TRANSFER, product.getId(), 5, warehouseId, destId, null, null, null);

        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isConflict());
    }

    @Test
    void auditEntryCreatedOnProductCreate() throws Exception {
        long before = auditEntryRepository.count();

        ProductCreate create = new ProductCreate(
                "Audited", "AUD-001", "general", new BigDecimal("1.00"), warehouseId, 0, List.of());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated());

        assertThat(auditEntryRepository.count()).isGreaterThan(before);
        assertThat(auditEntryRepository.findAll().stream()
                        .anyMatch(e -> "PRODUCT_CREATED".equals(e.getAction())))
                .isTrue();
    }

    @Test
    void checkoutCreatesTransactionAndDeductsStock() throws Exception {
        Product product = productRepository.save(Product.create(
                "POS",
                "POS-INT-001",
                "general",
                new BigDecimal("10.00"),
                warehouseRepository.findById(warehouseId).orElseThrow(),
                1,
                new String[0]));
        seedStock(product.getId(), 10);

        TransactionCreate checkout = new TransactionCreate(
                List.of(new CartItemDto(
                        product.getId(), product.getName(), product.getSku(), 2, product.getPrice())),
                new BigDecimal("20.00"),
                new BigDecimal("3.20"),
                new BigDecimal("23.20"));

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.CASHIER_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkout)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(23.20));

        mockMvc.perform(get("/api/v1/products/{id}", product.getId())
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.CASHIER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(8));
    }

    @Test
    void checkoutOversellReturns409() throws Exception {
        Product product = productRepository.save(Product.create(
                "Oversell",
                "OVER-001",
                "general",
                new BigDecimal("10.00"),
                warehouseRepository.findById(warehouseId).orElseThrow(),
                1,
                new String[0]));
        seedStock(product.getId(), 1);

        TransactionCreate checkout = new TransactionCreate(
                List.of(new CartItemDto(
                        product.getId(), product.getName(), product.getSku(), 5, product.getPrice())),
                new BigDecimal("50.00"),
                new BigDecimal("8.00"),
                new BigDecimal("58.00"));

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.CASHIER_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkout)))
                .andExpect(status().isConflict());
    }

    @Test
    void cashierAndAdminCanListTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.CASHIER_ID)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.ADMIN_ID)))
                .andExpect(status().isOk());
    }

    private void seedStock(UUID productId, int qty) throws Exception {
        StockMovementCreate in =
                new StockMovementCreate(StockMovementType.IN, productId, qty, null, warehouseId, "Test Supplier", null, "seed");
        mockMvc.perform(post("/api/v1/stock-movements")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.WORKER_ID))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(in)))
                .andExpect(status().isCreated());
    }
}
