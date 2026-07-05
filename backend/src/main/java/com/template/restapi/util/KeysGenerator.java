/**
 * #############################################################################
 * ### Asymmetric (RSA) key generator
 * ###
 * ### @file KeysGenerator.java
 * ### @author Sebastian Russo
 * ### @date 2026
 * #############################################################################
 *
 * Generates a public/private RSA key pair and writes them to PEM files. Run
 * once; the private key must never be shared.
 *
 * Run it standalone to produce the keys, then paste their contents into the
 * E_PRIVATE_KEY / E_PUBLIC_KEY environment variables consumed by EnDeCrypt:
 *
 *   # from the project root, after building once so classes exist:
 *   java -cp build/classes/java/main com.template.restapi.util.KeysGenerator
 *
 * PEM formats produced:
 *   - private key: PKCS#8  ("BEGIN PRIVATE KEY")  -- unencrypted (JDK native)
 *   - public  key: X.509   ("BEGIN PUBLIC KEY")
 *
 * No external dependencies (pure JDK). Adapted from java_various_utils/KeysGenerator.java.
 */
package com.template.restapi.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public final class KeysGenerator {

    private KeysGenerator() { }

    public static void generateRsaKeys(String privatePath, String publicPath) throws Exception {
        if (privatePath == null || privatePath.trim().isEmpty() || privatePath.contains(".."))
            throw new IllegalArgumentException("Invalid file path");

        if (publicPath == null || publicPath.trim().isEmpty() || publicPath.contains(".."))
            throw new IllegalArgumentException("Invalid file path");

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048); // Can be modified (e.g. 3072, 4096)
        KeyPair keyPair = generator.generateKeyPair();

        byte[] privDer = keyPair.getPrivate().getEncoded();  // PKCS#8
        byte[] pubDer = keyPair.getPublic().getEncoded();    // SubjectPublicKeyInfo

        Files.write(Paths.get(privatePath),
                pemEncode("PRIVATE KEY", privDer).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(publicPath),
                pemEncode("PUBLIC KEY", pubDer).getBytes(StandardCharsets.UTF_8));

        System.out.println("Keys generated successfully!");
        System.out.println("Private key saved as " + privatePath);
        System.out.println("Public key saved as " + publicPath);
    }

    // Wraps DER bytes into a 64-column PEM block.
    private static String pemEncode(String label, byte[] der) {
        String b64 = Base64.getEncoder().encodeToString(der);
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(label).append("-----").append(System.lineSeparator());
        int pos = 0;
        while (pos < b64.length()) {
            int take = Math.min(64, b64.length() - pos);
            sb.append(b64, pos, pos + take).append(System.lineSeparator());
            pos += take;
        }
        sb.append("-----END ").append(label).append("-----").append(System.lineSeparator());
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        String privatePath = "private_rsa_key.pem";
        String publicPath = "public_rsa_key.pem";
        generateRsaKeys(privatePath, publicPath);
    }
}
