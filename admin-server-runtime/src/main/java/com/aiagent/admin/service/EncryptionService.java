package com.aiagent.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Slf4j
@Service
public class EncryptionService {

    @Value("${jasypt.encryptor.password:}")
    private String encryptorPassword;

    private StringEncryptor encryptor;

    @PostConstruct
    public void init() {
        if (encryptorPassword == null || encryptorPassword.isEmpty()) {
            log.warn("JASYPT_ENCRYPTOR_PASSWORD not set, using fallback encryption");
            encryptorPassword = "default-password-change-in-production";
        }
        
        PooledPBEStringEncryptor pooledEncryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(encryptorPassword);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        pooledEncryptor.setConfig(config);
        
        this.encryptor = pooledEncryptor;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        if (isEncrypted(plainText)) {
            return plainText;
        }
        return "ENC(" + encryptor.encrypt(plainText) + ")";
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        if (!isEncrypted(encryptedText)) {
            return encryptedText;
        }
        String cipherText = encryptedText.substring(4, encryptedText.length() - 1);
        return encryptor.decrypt(cipherText);
    }

    public boolean isEncrypted(String text) {
        return text != null && text.startsWith("ENC(") && text.endsWith(")");
    }
}
