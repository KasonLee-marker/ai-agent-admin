package com.aiagent.admin.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EncryptionService {

    @Value("${jasypt.encryptor.password:default-password-change-in-production}")
    private String encryptorPassword;

    @Value("${jasypt.encryptor.algorithm:PBEWITHHMACSHA512ANDAES_256}")
    private String algorithm;

    private StringEncryptor encryptor;

    @PostConstruct
    public void init() {
        log.info("Initializing EncryptionService with password: {}***",
                encryptorPassword != null && encryptorPassword.length() > 5
                        ? encryptorPassword.substring(0, 5) : "default");

        this.encryptor = getPooledPBEStringEncryptor();

        // 测试加密解密是否正常
        try {
            String testPlain = "test-key-12345";
            String testEncrypted = encrypt(testPlain);
            String testDecrypted = decrypt(testEncrypted);
            if (testPlain.equals(testDecrypted)) {
                log.info("EncryptionService initialized successfully, encryption test passed");
            } else {
                log.error("EncryptionService initialization FAILED: encrypt/decrypt mismatch!");
            }
        } catch (Exception e) {
            log.error("EncryptionService initialization FAILED: {}", e.getMessage());
        }
    }

    /**
     * 获取 PooledPBEStringEncryptor
     *
     * @return PooledPBEStringEncryptor
     */
    private @NonNull PooledPBEStringEncryptor getPooledPBEStringEncryptor() {
        PooledPBEStringEncryptor pooledEncryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(encryptorPassword);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        pooledEncryptor.setConfig(config);
        return pooledEncryptor;
    }

    /**
     * 加密字符串
     *
     * @param plainText 明文字符串
     * @return 加密后的字符串
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        if (isEncrypted(plainText)) {
            return plainText;
        }
        String encrypted = encryptor.encrypt(plainText);
        return "ENC(" + encrypted + ")";
    }

    /**
     *  解密字符串
     * @param encryptedText 加密后的字符串
     * @return 解密后的字符串
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        if (!isEncrypted(encryptedText)) {
            log.debug("Text is not encrypted format, returning as-is");
            return encryptedText;
        }
        try {
            String cipherText = encryptedText.substring(4, encryptedText.length() - 1);
            return encryptor.decrypt(cipherText);
        } catch (Exception e) {
            log.error("Decryption failed for text starting with '{}...': {}",
                    encryptedText.substring(0, Math.min(20, encryptedText.length())),
                    e.getMessage());
            // 返回原始值而不是抛出异常，避免应用崩溃
            return encryptedText;
        }
    }

    public boolean isEncrypted(String text) {
        return text != null && text.startsWith("ENC(") && text.endsWith(")");
    }
}
