package com.prodwatch.api.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prodwatch.api.util.CustomLogger;

import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;

/**
 * Application-level JSON cache-aside helper. No-ops when Redis is disabled or unavailable.
 * Controllers must not use this — inject into services only.
 */
@Service
public class RedisCacheService {

    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;

    public RedisCacheService(RedisClient redisClient, ObjectMapper objectMapper) {
        this.redisClient = redisClient;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> cacheGet(String key, Class<T> type) {
        if (!redisClient.isEnabled()) {
            return Optional.empty();
        }
        Jedis jedis = redisClient.borrowConnection();
        if (jedis == null) {
            return Optional.empty();
        }
        try {
            String json = jedis.get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (Exception ex) {
            CustomLogger.debug("Redis cache get failed for " + key + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public <T> Optional<List<T>> cacheGetList(String key, Class<T> elementType) {
        if (!redisClient.isEnabled()) {
            return Optional.empty();
        }
        Jedis jedis = redisClient.borrowConnection();
        if (jedis == null) {
            return Optional.empty();
        }
        try {
            String json = jedis.get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(
                    json, objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)));
        } catch (Exception ex) {
            CustomLogger.debug("Redis cache get list failed for " + key + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public void cacheSet(String key, Object value, int ttlSeconds) {
        if (!redisClient.isEnabled() || ttlSeconds <= 0) {
            return;
        }
        Jedis jedis = redisClient.borrowConnection();
        if (jedis == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            jedis.setex(key, ttlSeconds, json);
        } catch (Exception ex) {
            CustomLogger.debug("Redis cache set failed for " + key + ": " + ex.getMessage());
        }
    }

    public void cacheDelete(String key) {
        if (!redisClient.isEnabled()) {
            return;
        }
        Jedis jedis = redisClient.borrowConnection();
        if (jedis == null) {
            return;
        }
        try {
            jedis.del(key);
        } catch (Exception ex) {
            CustomLogger.debug("Redis cache delete failed for " + key + ": " + ex.getMessage());
        }
    }

    /** Deletes all keys matching {@code prefix}* (SCAN, not KEYS). */
    public void cacheDeleteByPrefix(String prefix) {
        if (!redisClient.isEnabled() || prefix == null || prefix.isBlank()) {
            return;
        }
        Jedis jedis = redisClient.borrowConnection();
        if (jedis == null) {
            return;
        }
        try {
            String pattern = prefix.endsWith("*") ? prefix : prefix + "*";
            String cursor = "0";
            List<String> toDelete = new ArrayList<>();
            do {
                var scanResult = jedis.scan(cursor, new redis.clients.jedis.params.ScanParams().match(pattern).count(100));
                cursor = scanResult.getCursor();
                toDelete.addAll(scanResult.getResult());
            } while (!"0".equals(cursor));

            if (!toDelete.isEmpty()) {
                jedis.del(toDelete.toArray(String[]::new));
            }
        } catch (Exception ex) {
            CustomLogger.debug("Redis cache delete by prefix failed for " + prefix + ": " + ex.getMessage());
        }
    }
}
