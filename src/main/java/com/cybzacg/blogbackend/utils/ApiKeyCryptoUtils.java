package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * API Key 可逆加解密工具。
 *
 * <p>用于加密数据库中需要再次取出使用的第三方 API Key。密钥应从环境变量或安全配置读取，
 * 不应硬编码在代码、脚本或数据库中。
 */
public final class ApiKeyCryptoUtils {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final String ENCRYPTED_PREFIX = "enc:v1:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ApiKeyCryptoUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 加密 API Key 明文。
     *
     * @param plainText API Key 明文
     * @param secretKey 加密主密钥
     * @return 带版本前缀的密文
     */
    public static String encrypt(String plainText, String secretKey) {
        ExceptionThrowerCore.throwBusinessIfBlank(
            plainText,
            ResultErrorCode.PARAM_VALIDATION_FAILED,
            "API Key 明文不能为空"
        );
        byte[] iv = new byte[IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(
                Cipher.ENCRYPT_MODE,
                buildSecretKey(secretKey),
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            );
            byte[] encrypted = cipher.doFinal(
                plainText.getBytes(StandardCharsets.UTF_8)
            );
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return ENCRYPTED_PREFIX +
                Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            return ExceptionThrowerCore.throwBusiness(
                ResultErrorCode.SYSTEM_ERROR,
                "API Key 加密失败",
                ex
            );
        }
    }

    /**
     * 解密 API Key 密文。
     *
     * @param cipherText 带版本前缀的密文
     * @param secretKey  加密主密钥
     * @return API Key 明文
     */
    public static String decrypt(String cipherText, String secretKey) {
        ExceptionThrowerCore.throwBusinessIfBlank(
            cipherText,
            ResultErrorCode.PARAM_VALIDATION_FAILED,
            "API Key 密文不能为空"
        );
        ExceptionThrowerCore.throwBusinessIf(
            !isEncrypted(cipherText),
            ResultErrorCode.PARAM_FORMAT_INVALID,
            "API Key 密文格式无效"
        );

        try {
            byte[] payload = Base64.getUrlDecoder()
                .decode(cipherText.substring(ENCRYPTED_PREFIX.length()));
            if (payload.length <= IV_LENGTH_BYTES) {
                ExceptionThrowerCore.throwBusinessEx(
                    ResultErrorCode.PARAM_FORMAT_INVALID,
                    "API Key 密文内容无效"
                );
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(
                Cipher.DECRYPT_MODE,
                buildSecretKey(secretKey),
                new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            );
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return ExceptionThrowerCore.throwBusiness(
                ResultErrorCode.PARAM_FORMAT_INVALID,
                "API Key 密文格式无效",
                ex
            );
        } catch (GeneralSecurityException ex) {
            return ExceptionThrowerCore.throwBusiness(
                ResultErrorCode.SYSTEM_ERROR,
                "API Key 解密失败",
                ex
            );
        }
    }

    /**
     * 兼容旧明文数据：已加密则解密，未加密则原样返回。
     */
    public static String decryptIfEncrypted(String value, String secretKey) {
        if (value == null) {
            return null;
        }
        return isEncrypted(value) ? decrypt(value, secretKey) : value;
    }

    /**
     * 判断字符串是否为本工具生成的密文。
     */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }

    private static SecretKeySpec buildSecretKey(String secretKey) {
        ExceptionThrowerCore.throwBusinessIfBlank(
            secretKey,
            ResultErrorCode.PARAM_VALIDATION_FAILED,
            "API Key 加密主密钥不能为空"
        );
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] keyBytes = digest.digest(
                secretKey.getBytes(StandardCharsets.UTF_8)
            );
            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        } catch (GeneralSecurityException ex) {
            return ExceptionThrowerCore.throwBusiness(
                ResultErrorCode.SYSTEM_ERROR,
                "API Key 加密主密钥初始化失败",
                ex
            );
        }
    }
}
