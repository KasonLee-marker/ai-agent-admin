package com.aiagent.model.service;

import com.ulisesbocchio.jasyptspringboot.encryptor.DefaultLazyEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
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
        this.encryptor = new DefaultLazyEncryptor(() -> encryptorPassword);
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
