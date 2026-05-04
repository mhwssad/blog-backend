package com.cybzacg.blogbackend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cybzacg.blogbackend.exception.BusinessException;
import org.junit.jupiter.api.Test;

class ApiKeyCryptoUtilsTest {

    private static final String SECRET_KEY =
        "local-test-secret-key-with-enough-entropy";

    @Test
    void encryptAndDecryptShouldReturnOriginalApiKey() {
        String apiKey = "sk-test-1234567890";

        String encrypted = ApiKeyCryptoUtils.encrypt(apiKey, SECRET_KEY);

        assertTrue(ApiKeyCryptoUtils.isEncrypted(encrypted));
        assertNotEquals(apiKey, encrypted);
        assertEquals(apiKey, ApiKeyCryptoUtils.decrypt(encrypted, SECRET_KEY));
    }

    @Test
    void encryptShouldUseRandomIv() {
        String apiKey = "sk-test-random-iv";

        String first = ApiKeyCryptoUtils.encrypt(apiKey, SECRET_KEY);
        String second = ApiKeyCryptoUtils.encrypt(apiKey, SECRET_KEY);

        assertNotEquals(first, second);
        assertEquals(apiKey, ApiKeyCryptoUtils.decrypt(first, SECRET_KEY));
        assertEquals(apiKey, ApiKeyCryptoUtils.decrypt(second, SECRET_KEY));
    }

    @Test
    void decryptIfEncryptedShouldKeepPlainTextForLegacyValue() {
        String legacyPlainText = "legacy-api-key";

        assertEquals(
            legacyPlainText,
            ApiKeyCryptoUtils.decryptIfEncrypted(legacyPlainText, SECRET_KEY)
        );
    }

    @Test
    void decryptShouldRejectInvalidCipherText() {
        assertThrows(
            BusinessException.class,
            () -> ApiKeyCryptoUtils.decrypt("plain-api-key", SECRET_KEY)
        );
    }

    @Test
    void encryptShouldRejectBlankSecretKey() {
        assertThrows(
            BusinessException.class,
            () -> ApiKeyCryptoUtils.encrypt("sk-test", " ")
        );
    }
}
