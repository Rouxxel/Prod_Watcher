package com.prodwatch.api.cache;

import java.util.concurrent.atomic.AtomicReference;

import com.prodwatch.api.util.CustomLogger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

/**
 * Optional Redis connection holder. Off by default ({@code REDIS_ENABLED=false}).
 * Connection failures are logged; startup never depends on Redis.
 */
@Component
public class RedisClient {

    public static final String STATUS_DISABLED = "disabled";
    public static final String STATUS_CONNECTED = "connected";
    public static final String STATUS_UNAVAILABLE = "unavailable";

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String password;
    private final int database;
    private final boolean tls;

    private final AtomicReference<String> status = new AtomicReference<>(STATUS_DISABLED);
    private volatile Jedis jedis;

    public RedisClient(
            @Value("${REDIS_ENABLED:false}") boolean enabled,
            @Value("${REDIS_HOST:localhost}") String host,
            @Value("${REDIS_PORT:6379}") int port,
            @Value("${REDIS_PASSWORD:}") String password,
            @Value("${REDIS_DB:0}") int database,
            @Value("${REDIS_TLS:false}") boolean tls) {
        this.enabled = enabled;
        this.host = host == null ? "localhost" : host.trim();
        this.port = port;
        this.password = password == null ? "" : password.trim();
        this.database = database;
        this.tls = tls;

        if (!this.enabled) {
            CustomLogger.info("Redis is disabled (REDIS_ENABLED=false)");
            status.set(STATUS_DISABLED);
            return;
        }

        tryConnect();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getStatus() {
        return status.get();
    }

    /**
     * Returns a live Jedis connection, reconnecting lazily on failure.
     * Returns {@code null} when disabled or unavailable.
     */
    public Jedis borrowConnection() {
        if (!enabled) {
            return null;
        }
        Jedis current = jedis;
        if (current != null) {
            try {
                current.ping();
                return current;
            } catch (Exception ex) {
                CustomLogger.debug("Redis ping failed, reconnecting: " + ex.getMessage());
                closeQuietly(current);
                jedis = null;
            }
        }
        return tryConnect();
    }

    private Jedis tryConnect() {
        try {
            JedisClientConfig config = buildClientConfig();
            Jedis connection = new Jedis(new HostAndPort(host, port), config);
            connection.ping();
            jedis = connection;
            status.set(STATUS_CONNECTED);
            CustomLogger.info("Redis connected at " + host + ":" + port + "/" + database
                    + (tls ? " (TLS)" : ""));
            return connection;
        } catch (Exception ex) {
            status.set(STATUS_UNAVAILABLE);
            CustomLogger.warning("Redis unavailable: " + ex.getMessage());
            jedis = null;
            return null;
        }
    }

    private JedisClientConfig buildClientConfig() {
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .database(database)
                .connectionTimeoutMillis(2_000)
                .socketTimeoutMillis(2_000);
        if (!password.isEmpty()) {
            builder.password(password);
        }
        if (tls) {
            builder.ssl(true);
        }
        return builder.build();
    }

    private static void closeQuietly(Jedis connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Exception ignored) {
            // best effort
        }
    }
}
