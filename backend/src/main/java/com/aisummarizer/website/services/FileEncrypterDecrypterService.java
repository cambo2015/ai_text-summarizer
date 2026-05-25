package com.aisummarizer.website.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class FileEncrypterDecrypterService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION = "PBKDF2WithHmacSHA256";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;

    @Value("${encryption.key}")
    private String secretKey;

    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            SecretKey aesKey = deriveKey(secretKey, salt);

            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);

            byte[] cipherText =
                    cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(
                    SALT_LENGTH + IV_LENGTH + cipherText.length);

            buffer.put(salt);
            buffer.put(iv);
            buffer.put(cipherText);

            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] salt = new byte[SALT_LENGTH];
            buffer.get(salt);

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            SecretKey aesKey = deriveKey(secretKey, salt);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory =
                SecretKeyFactory.getInstance(KEY_DERIVATION);

        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                AES_KEY_SIZE
        );

        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
