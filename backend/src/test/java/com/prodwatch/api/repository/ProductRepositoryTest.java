package com.prodwatch.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.prodwatch.api.entity.Product;
import com.prodwatch.api.entity.Warehouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Test
    void skuMustBeUnique() {
        Warehouse warehouse = warehouseRepository.save(Warehouse.create("Main", "Floor 1"));

        productRepository.save(product(warehouse, "SKU-UNIQUE-1"));
        assertThat(productRepository.existsBySku("SKU-UNIQUE-1")).isTrue();

        assertThatThrownBy(() -> productRepository.saveAndFlush(product(warehouse, "SKU-UNIQUE-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySkuReturnsProduct() {
        Warehouse warehouse = warehouseRepository.save(Warehouse.create("Secondary", "Floor 2"));
        Product saved = productRepository.save(product(warehouse, "SKU-FIND-ME"));

        assertThat(productRepository.findBySku("SKU-FIND-ME"))
                .isPresent()
                .get()
                .extracting(Product::getId)
                .isEqualTo(saved.getId());
    }

    private static Product product(Warehouse warehouse, String sku) {
        return Product.create("Test product", sku, "general", new BigDecimal("9.99"), warehouse, 5, new String[0]);
    }
}
