package com.aiagent.model.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    @InjectMocks
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(encryptionService, "encryptorPassword", "test-password");
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
}
