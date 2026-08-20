package com.prodwatch.api.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodwatch.api.core_specs.configuration.ConfigLoader;

import org.junit.jupiter.api.Test;

class RedisCacheServiceTest {

    @Test
    void disabledRedisCacheIsNoOp() {
        RedisClient client = new RedisClient(false, "localhost", 6379, "", 0, false);
        RedisCacheService cacheService =
                new RedisCacheService(client, new ObjectMapper());

        cacheService.cacheSet("test:key", "value", 60);
        assertThat(cacheService.cacheGet("test:key", String.class)).isEmpty();
        assertThat(client.getStatus()).isEqualTo(RedisClient.STATUS_DISABLED);
    }

    @Test
    void ttlConfigLoadsFromConfigFile() {
        assertThat(ConfigLoader.redisCacheTtlSeconds("profile", 0)).isEqualTo(600);
        assertThat(ConfigLoader.redisCacheTtlSeconds("settings", 0)).isEqualTo(1800);
        assertThat(ConfigLoader.redisCacheTtlSeconds("movement", 0)).isEqualTo(86400);
        assertThat(ConfigLoader.redisCacheTtlSeconds("stock_display", 0)).isEqualTo(15);
        assertThat(ConfigLoader.redisCacheTtlSeconds("missing_key", 99)).isEqualTo(99);
    }

    @Test
    void cacheKeysAreTenantScoped() {
        var ecosystemId = java.util.UUID.fromString("33333333-3333-4333-8333-333333333301");
        var productId = java.util.UUID.fromString("22222222-2222-4222-8222-222222222201");
        var warehouseId = java.util.UUID.fromString("11111111-1111-4111-8111-111111111101");

        assertThat(RedisCacheKeys.settings(ecosystemId)).isEqualTo("settings:" + ecosystemId);
        assertThat(RedisCacheKeys.productMeta(ecosystemId, productId))
                .isEqualTo("product:meta:" + ecosystemId + ":" + productId);
        assertThat(RedisCacheKeys.stockDisplay(ecosystemId, productId, warehouseId))
                .isEqualTo("stock:" + ecosystemId + ":" + productId + ":" + warehouseId);
    }
}
