package com.prodwatch.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import com.prodwatch.api.entity.AppRole;
import com.prodwatch.api.entity.Ecosystem;
import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.Profile;
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
class EcosystemIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private EcosystemRepository ecosystemRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    private UUID otherProductId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        TestFixtures.seedUser(
                profileRepository, userRoleRepository, ecosystemRepository, TestFixtures.ADMIN_ID, AppRole.admin);

        Ecosystem otherEcosystem = TestFixtures.otherEcosystem(ecosystemRepository);
        TestFixtures.seedUserInEcosystem(
                profileRepository, userRoleRepository, otherEcosystem, TestFixtures.OTHER_ADMIN_ID, AppRole.admin);
        otherUserId = TestFixtures.OTHER_ADMIN_ID;

        Warehouse otherWarehouse =
                TestFixtures.saveWarehouse(warehouseRepository, otherEcosystem, "Other WH", "Remote");
        Product otherProduct = productRepository.save(Product.create(
                "Other Mug",
                "OTHER-MUG",
                "Kitchen",
                new BigDecimal("9.99"),
                otherWarehouse,
                5,
                new String[0]));
        otherProductId = otherProduct.getId();
    }

    @Test
    void crossEcosystemProductByIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + otherProductId)
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.ADMIN_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossEcosystemUserByIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + otherUserId)
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.ADMIN_ID)))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersMeIncludesEcosystem() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", TestFixtures.bearerHeader(TestFixtures.ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecosystemId").value(TestFixtures.DEMO_ECOSYSTEM_ID.toString()))
                .andExpect(jsonPath("$.ecosystemName").value("Acme Demo Test"));
    }

    @Test
    void userWithoutEcosystemIsRejected() throws Exception {
        UUID orphanId = UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd");
        profileRepository.save(Profile.create(orphanId, "orphan@test.local", "Orphan", true));
        userRoleRepository.save(new com.prodwatch.api.entity.UserRole(
                profileRepository.findById(orphanId).orElseThrow(), AppRole.admin));

        mockMvc.perform(get("/api/v1/products").header("Authorization", TestFixtures.bearerHeader(orphanId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listProductsOnlyReturnsOwnEcosystem() throws Exception {
        Warehouse demoWarehouse =
                TestFixtures.saveWarehouse(warehouseRepository, ecosystemRepository, "Demo WH", "Local");
        productRepository.save(Product.create(
                "Demo Mug",
                "DEMO-MUG",
                "Kitchen",
                new BigDecimal("12.00"),
                demoWarehouse,
                3,
                new String[0]));

        mockMvc.perform(get("/api/v1/products").header("Authorization", TestFixtures.bearerHeader(TestFixtures.ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("DEMO-MUG");
                    assertThat(body).doesNotContain("OTHER-MUG");
                });
    }
}
