package com.prodwatch.api.cache;

import com.prodwatch.api.core_specs.configuration.ConfigLoader;

import org.springframework.stereotype.Component;

/** TTL seconds for each named cache bucket — sourced from config_file.json. */
@Component
public class RedisCacheTtls {

    public int profile() {
        return ConfigLoader.redisCacheTtlSeconds("profile", 600);
    }

    public int settings() {
        return ConfigLoader.redisCacheTtlSeconds("settings", 1800);
    }

    public int warehousesList() {
        return ConfigLoader.redisCacheTtlSeconds("warehouses_list", 3600);
    }

    public int warehouse() {
        return ConfigLoader.redisCacheTtlSeconds("warehouse", 3600);
    }

    public int productMeta() {
        return ConfigLoader.redisCacheTtlSeconds("product_meta", 1800);
    }

    public int ecosystem() {
        return ConfigLoader.redisCacheTtlSeconds("ecosystem", 21600);
    }

    public int movement() {
        return ConfigLoader.redisCacheTtlSeconds("movement", 86400);
    }

    public int audit() {
        return ConfigLoader.redisCacheTtlSeconds("audit", 86400);
    }

    public int transaction() {
        return ConfigLoader.redisCacheTtlSeconds("transaction", 86400);
    }

    public int stockDisplay() {
        return ConfigLoader.redisCacheTtlSeconds("stock_display", 15);
    }
}
