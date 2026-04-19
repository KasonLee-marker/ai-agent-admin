package com.aiagent.admin.service;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EncryptionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    @InjectMocks
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // 设置配置值并调用 init()
        ReflectionTestUtils.setField(encryptionService, "encryptorPassword", "test-password");
        ReflectionTestUtils.setField(encryptionService, "algorithm", "PBEWITHHMACSHA512ANDAES_256");
        encryptionService.init();
    }

    @Test
    void testEncryptDecrypt() {
        String plainText = "my-secret-api-key-12345";

        String encrypted = encryptionService.encrypt(plainText);
        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC("));
        assertTrue(encrypted.endsWith(")"));
        assertNotEquals(plainText, encrypted);

        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncryptNullOrEmpty() {
        assertNull(encryptionService.encrypt(null));
        assertEquals("", encryptionService.encrypt(""));
    }

    @Test
    void testDecryptNullOrEmpty() {
        assertNull(encryptionService.decrypt(null));
        assertEquals("", encryptionService.decrypt(""));
    }

    @Test
    void testDecryptPlainText() {
        String plainText = "plain-text-not-encrypted";
        assertEquals(plainText, encryptionService.decrypt(plainText));
    }

    @Test
    void testIsEncrypted() {
        assertTrue(encryptionService.isEncrypted("ENC(encrypted-text)"));
        assertFalse(encryptionService.isEncrypted("plain-text"));
        assertFalse(encryptionService.isEncrypted(null));
        assertFalse(encryptionService.isEncrypted(""));
        assertFalse(encryptionService.isEncrypted("ENC("));
        assertFalse(encryptionService.isEncrypted("text)"));
    }

    @Test
    void testDoubleEncryption() {
        String plainText = "test-key";
        String encrypted1 = encryptionService.encrypt(plainText);
        String encrypted2 = encryptionService.encrypt(encrypted1);

        // Double encryption should return the same as single encryption
        assertEquals(encrypted1, encrypted2);
    }

    @Test
    void testDecryptFailureReturnsOriginalValue() {
        // 当解密失败时，应该返回原始加密值而不是抛出异常
        String invalidEncrypted = "ENC(invalid-cipher-text)";

        String result = encryptionService.decrypt(invalidEncrypted);
        assertEquals(invalidEncrypted, result);
    }

    @Test
    void testDecryptWithDifferentPassword() {
        // 使用不同密码加密的值无法解密
        String plainText = "test-key";

        // 用 test-password 加密
        String encrypted = encryptionService.encrypt(plainText);

        // 创建一个新的 encryptor 用不同密码
        PooledPBEStringEncryptor otherEncryptor = getPooledPBEStringEncryptor();

        EncryptionService otherService = new EncryptionService();
        ReflectionTestUtils.setField(otherService, "encryptor", otherEncryptor);

        // 使用不同密码的 service 应该无法解密
        String result = otherService.decrypt(encrypted);
        assertEquals(encrypted, result); // 应该返回原始加密值
    }

    /**
     * 获取一个 PooledPBEStringEncryptor 实例用于测试
     *
     * @return PooledPBEStringEncryptor
     */
    private static @NonNull PooledPBEStringEncryptor getPooledPBEStringEncryptor() {
        PooledPBEStringEncryptor otherEncryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword("different-password");
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        otherEncryptor.setConfig(config);
        return otherEncryptor;
    }
}
