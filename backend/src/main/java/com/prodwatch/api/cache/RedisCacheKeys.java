package com.prodwatch.api.cache;

import java.util.UUID;

/** Canonical Redis key builders — tenant-scoped where required. */
public final class RedisCacheKeys {

    private RedisCacheKeys() {}

    public static String profile(UUID userId) {
        return "profile:" + userId;
    }

    public static String settings(UUID ecosystemId) {
        return "settings:" + ecosystemId;
    }

    public static String warehousesList(UUID ecosystemId) {
        return "warehouses:" + ecosystemId;
    }

    public static String warehouse(UUID ecosystemId, UUID warehouseId) {
        return "warehouse:" + ecosystemId + ":" + warehouseId;
    }

    public static String productMeta(UUID ecosystemId, UUID productId) {
        return "product:meta:" + ecosystemId + ":" + productId;
    }

    public static String ecosystem(UUID ecosystemId) {
        return "ecosystem:" + ecosystemId;
    }

    public static String movement(UUID movementId) {
        return "movement:" + movementId;
    }

    public static String audit(UUID auditId) {
        return "audit:" + auditId;
    }

    public static String transaction(UUID transactionId) {
        return "txn:" + transactionId;
    }

    public static String stockDisplay(UUID ecosystemId, UUID productId, UUID warehouseId) {
        return "stock:" + ecosystemId + ":" + productId + ":" + warehouseId;
    }

    /** Prefix for pattern deletes, e.g. {@code warehouses:{ecosystemId}}*. */
    public static String warehousesListPrefix(UUID ecosystemId) {
        return "warehouses:" + ecosystemId;
    }
}
