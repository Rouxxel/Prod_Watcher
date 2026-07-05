/**
 * #############################################################################
 * ### RSA Encrypt/Decrypt utility
 * ###
 * ### @file EnDeCrypt.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Encrypts/decrypts strings with RSA-OAEP (SHA-256). Keys are read from environment variables:
 *
 *   E_PUBLIC_KEY        PEM public key  ("BEGIN PUBLIC KEY",  X.509/SPKI)
 *   E_PRIVATE_KEY       PEM private key ("BEGIN PRIVATE KEY", PKCS#8, unencrypted)
 *   E_PRIVATE_PASSWORD  (optional) kept for parity; see note below
 *
 * Generate the keys once with util/KeysGenerator, then place their PEM contents
 * in those variables (.env). PEM newlines may be encoded as literal "\n".
 *
 * IMPORTANT (vs. Python):
 *   The bundled KeysGenerator emits an UNENCRYPTED PKCS#8 private key, because
 *   loading a password-protected PKCS#8 key with the pure JDK is not
 *   straightforward (it needs BouncyCastle). E_PRIVATE_PASSWORD is therefore
 *   accepted but ignored here. If you require an encrypted private key, add
 *   the BouncyCastle dependency and decrypt it before building the KeyFactory.
 *
 * Keys are loaded lazily on first use, so the application can start without
 * them (unlike the Python module, which loaded at import time). Methods throw
 * IllegalStateException if used while the env vars are unset.
 *
 * No external dependencies (pure JDK).
 */
package com.template.restapi.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public final class EnDeCrypt {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    private static volatile PublicKey publicKey;
    private static volatile PrivateKey privateKey;

    private EnDeCrypt() { }

    /** OAEP with SHA-256 for both the digest and the MGF1 mask (matches Python). */
    private static OAEPParameterSpec oaepParams() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT);
    }

    private static PublicKey publicKey() {
        if (publicKey == null) {
            synchronized (EnDeCrypt.class) {
                if (publicKey == null) {
                    String pem = requireEnv("E_PUBLIC_KEY");
                    try {
                        byte[] der = pemToDer(pem);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        publicKey = kf.generatePublic(new X509EncodedKeySpec(der));
                        CustomLogger.info("Public key loaded successfully.");
                    } catch (Exception e) {
                        CustomLogger.error("Failed to load public key: " + e.getMessage());
                        throw new IllegalStateException("Failed to load E_PUBLIC_KEY", e);
                    }
                }
            }
        }
        return publicKey;
    }

    private static PrivateKey privateKey() {
        if (privateKey == null) {
            synchronized (EnDeCrypt.class) {
                if (privateKey == null) {
                    String pem = requireEnv("E_PRIVATE_KEY");
                    try {
                        byte[] der = pemToDer(pem);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(der));
                        CustomLogger.info("Private key loaded successfully.");
                    } catch (Exception e) {
                        CustomLogger.error("Failed to load private key: " + e.getMessage());
                        throw new IllegalStateException("Failed to load E_PRIVATE_KEY", e);
                    }
                }
            }
        }
        return privateKey;
    }

    /**
     * Encrypt a string with the public key. Returns Base64-encoded ciphertext.
     * (For non-string data, serialize to JSON first, then encrypt the string.)
     */
    public static String encryptIn(String message) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey(), oaepParams());
            byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
            CustomLogger.debug("Encryption successful");
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            CustomLogger.error("Encryption failed: " + e.getMessage());
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /** Decrypt a Base64-encoded ciphertext with the private key. Returns the plaintext string. */
    public static String decryptOut(String token) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey(), oaepParams());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(token));
            CustomLogger.debug("Decryption successful");
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            CustomLogger.error("Decryption failed: " + e.getMessage());
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    // --- helpers ---
    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " environment variable is not set.");
        }
        return value;
    }

    /** Strips PEM armor/whitespace (and literal "\n") and Base64-decodes to DER. */
    private static byte[] pemToDer(String pem) {
        String cleaned = pem
                .replace("\\n", "\n")
                .replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(cleaned);
    }
}
