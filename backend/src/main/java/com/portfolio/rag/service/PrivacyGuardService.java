package com.portfolio.rag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PrivacyGuardService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?\\d{1,3}[ -]?)?\\(?\\d{3}\\)?[ -]?\\d{3}[ -]?\\d{3,4}");
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");

    private final SecretKeySpec secretKey;

    public PrivacyGuardService(@Value("${app.security.aes-key}") String rawKey) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(rawKey.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(Arrays.copyOf(keyBytes, 16), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AES key", e);
        }
    }

    public RedactionResult redactSensitiveData(String input) {
        if (input == null || input.isBlank()) {
            return new RedactionResult("", 0);
        }
        int count = 0;
        String scrubbed = input;

        Matcher emailMatcher = EMAIL_PATTERN.matcher(scrubbed);
        while (emailMatcher.find()) count++;
        scrubbed = emailMatcher.replaceAll("[REDACTED_EMAIL]");

        Matcher cardMatcher = CARD_PATTERN.matcher(scrubbed);
        while (cardMatcher.find()) count++;
        scrubbed = cardMatcher.replaceAll("[REDACTED_CARD]");

        Matcher phoneMatcher = PHONE_PATTERN.matcher(scrubbed);
        while (phoneMatcher.find()) count++;
        scrubbed = phoneMatcher.replaceAll("[REDACTED_PHONE]");

        return new RedactionResult(scrubbed, count);
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("AES Encryption failed", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES Decryption failed", e);
        }
    }

    public record RedactionResult(String sanitizedText, int redactionCount) {}
}
