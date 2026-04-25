package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp SSL 客户端工具类。
 *
 * <p>统一处理 SSL/TLS 客户端、证书信任管理器与超时配置，避免不同创建入口重复拼装
 * OkHttpClient.Builder。</p>
 */
@Slf4j
public class OkHttpSslUtil {
    /**
     * TLS 协议名称。
     */
    private static final String TLS_PROTOCOL = "TLS";

    /**
     * 连接超时时间（秒）。
     */
    private static final int CONNECT_TIMEOUT = 30;

    /**
     * 读取超时时间（秒）。
     */
    private static final int READ_TIMEOUT = 60;

    /**
     * 写入超时时间（秒）。
     */
    private static final int WRITE_TIMEOUT = 60;

    /**
     * 记录由本工具创建的 SSLContext 与其对应的 TrustManager，确保自定义证书场景下
     * SocketFactory 与 TrustManager 保持一致。
     */
    private static final Map<SSLContext, X509TrustManager> SSL_CONTEXT_TRUST_MANAGER_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private OkHttpSslUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 创建信任所有证书的 OkHttpClient，仅适用于开发或测试环境。
     *
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createUnsafeClient() {
        return createUnsafeClient(CONNECT_TIMEOUT, READ_TIMEOUT, WRITE_TIMEOUT);
    }

    /**
     * 创建信任所有证书的 OkHttpClient，仅适用于开发或测试环境。
     *
     * @param connectTimeoutSeconds 连接超时时间（秒）
     * @param readTimeoutSeconds    读取超时时间（秒）
     * @param writeTimeoutSeconds   写入超时时间（秒）
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createUnsafeClient(long connectTimeoutSeconds,
                                                  long readTimeoutSeconds,
                                                  long writeTimeoutSeconds) {
        log.warn("警告：正在创建信任所有证书的 OkHttpClient，仅用于开发/测试环境，生产环境请勿使用");
        try {
            X509TrustManager trustManager = createTrustAllManager();
            SSLContext sslContext = createSslContext(trustManager);
            return buildSslClient(sslContext.getSocketFactory(), trustManager, (hostname, session) -> true,
                    connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throwSslBusinessException("创建不安全 SSL 客户端失败", e);
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * 创建使用系统默认 SSL 配置的 OkHttpClient。
     *
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createSslClient() {
        return createSslClient(CONNECT_TIMEOUT, READ_TIMEOUT, WRITE_TIMEOUT);
    }

    /**
     * 创建使用系统默认 SSL 配置的 OkHttpClient。
     *
     * @param connectTimeoutSeconds 连接超时时间（秒）
     * @param readTimeoutSeconds    读取超时时间（秒）
     * @param writeTimeoutSeconds   写入超时时间（秒）
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createSslClient(long connectTimeoutSeconds,
                                               long readTimeoutSeconds,
                                               long writeTimeoutSeconds) {
        try {
            SSLContext sslContext = SSLContext.getDefault();
            return buildSslClient(sslContext.getSocketFactory(), requireDefaultTrustManager(),
                    HttpsURLConnection.getDefaultHostnameVerifier(),
                    connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds);
        } catch (NoSuchAlgorithmException e) {
            throwSslBusinessException("创建默认 SSL 客户端失败", e);
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * 创建自定义 SSL 配置的 OkHttpClient。
     *
     * @param sslContext            SSL 上下文
     * @param hostnameVerifier      主机名校验器，可为空；为空时回退到系统默认实现
     * @param connectTimeoutSeconds 连接超时时间（秒）
     * @param readTimeoutSeconds    读取超时时间（秒）
     * @param writeTimeoutSeconds   写入超时时间（秒）
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createCustomSslClient(SSLContext sslContext,
                                                     HostnameVerifier hostnameVerifier,
                                                     long connectTimeoutSeconds,
                                                     long readTimeoutSeconds,
                                                     long writeTimeoutSeconds) {
        if (sslContext == null) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "SSLContext不能为空");
        }
        X509TrustManager trustManager = resolveTrustManager(sslContext);
        return buildSslClient(sslContext.getSocketFactory(), trustManager, resolveHostnameVerifier(hostnameVerifier),
                connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds);
    }

    /**
     * 创建自定义 SSL 配置的 OkHttpClient（使用默认超时时间）。
     *
     * @param sslContext       SSL 上下文
     * @param hostnameVerifier 主机名校验器
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createCustomSslClient(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        return createCustomSslClient(sslContext, hostnameVerifier, CONNECT_TIMEOUT, READ_TIMEOUT, WRITE_TIMEOUT);
    }

    /**
     * 创建仅信任指定证书的 TrustManager。
     *
     * @param certificates 证书数组
     * @return X509TrustManager 实例
     */
    public static X509TrustManager createTrustManager(X509Certificate[] certificates) {
        if (certificates == null || certificates.length == 0) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "证书列表不能为空");
        }
        X509Certificate[] acceptedIssuers = certificates.clone();
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                validateCertificateChain(chain, acceptedIssuers, "客户端");
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                validateCertificateChain(chain, acceptedIssuers, "服务端");
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return acceptedIssuers.clone();
            }
        };
    }

    /**
     * 创建信任指定证书的 SSLContext，并缓存对应 TrustManager，供自定义客户端创建时复用。
     *
     * @param certificates 证书数组
     * @return SSLContext 实例
     */
    public static SSLContext createSslContext(X509Certificate[] certificates) {
        try {
            X509TrustManager trustManager = createTrustManager(certificates);
            SSLContext sslContext = createSslContext(trustManager);
            SSL_CONTEXT_TRUST_MANAGER_CACHE.put(sslContext, trustManager);
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throwSslBusinessException("创建 SSLContext 失败", e);
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * 创建统一的 OkHttpClient.Builder，集中处理超时与重试策略。
     */
    private static OkHttpClient.Builder createBaseBuilder(long connectTimeoutSeconds,
                                                          long readTimeoutSeconds,
                                                          long writeTimeoutSeconds) {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
    }

    /**
     * 组装带 SSL 配置的 OkHttpClient，避免各创建入口重复设置 Builder。
     */
    private static OkHttpClient buildSslClient(SSLSocketFactory sslSocketFactory,
                                               X509TrustManager trustManager,
                                               HostnameVerifier hostnameVerifier,
                                               long connectTimeoutSeconds,
                                               long readTimeoutSeconds,
                                               long writeTimeoutSeconds) {
        return createBaseBuilder(connectTimeoutSeconds, readTimeoutSeconds, writeTimeoutSeconds)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier(hostnameVerifier)
                .build();
    }

    /**
     * 从 SSLContext 推导应该使用的 TrustManager。
     *
     * <p>优先复用本工具创建 SSLContext 时缓存的自定义 TrustManager，若不存在则回退到
     * 系统默认 TrustManager。</p>
     */
    private static X509TrustManager resolveTrustManager(SSLContext sslContext) {
        X509TrustManager trustManager = SSL_CONTEXT_TRUST_MANAGER_CACHE.get(sslContext);
        return trustManager != null ? trustManager : requireDefaultTrustManager();
    }

    /**
     * 获取系统默认 TrustManager，失败时抛出统一业务异常。
     */
    private static X509TrustManager requireDefaultTrustManager() {
        try {
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager x509TrustManager) {
                    return x509TrustManager;
                }
            }
        } catch (Exception e) {
            throwSslBusinessException("获取默认 TrustManager 失败", e);
        }
        throwSslBusinessException("获取默认 TrustManager 失败", null);
        throw new IllegalStateException("unreachable");
    }

    /**
     * 创建信任所有证书的 TrustManager。
     */
    private static X509TrustManager createTrustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    /**
     * 创建 SSLContext 并绑定指定 TrustManager。
     */
    private static SSLContext createSslContext(X509TrustManager trustManager)
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        return sslContext;
    }

    /**
     * 兜底主机名校验器，避免调用方传空时重复分支处理。
     */
    private static HostnameVerifier resolveHostnameVerifier(HostnameVerifier hostnameVerifier) {
        return hostnameVerifier != null ? hostnameVerifier : HttpsURLConnection.getDefaultHostnameVerifier();
    }

    /**
     * 校验证书链是否全部命中指定信任证书。
     */
    private static void validateCertificateChain(X509Certificate[] chain,
                                                 X509Certificate[] acceptedIssuers,
                                                 String certificateOwner) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException(certificateOwner + "证书链为空");
        }
        for (X509Certificate certificate : chain) {
            boolean trusted = false;
            for (X509Certificate acceptedIssuer : acceptedIssuers) {
                if (certificate.equals(acceptedIssuer)) {
                    trusted = true;
                    break;
                }
            }
            if (!trusted) {
                throw new CertificateException(certificateOwner + "证书不被信任");
            }
        }
    }

    /**
     * 构建 SSL 相关统一业务异常，接入当前异常处理体系。
     */
    private static void throwSslBusinessException(String message, Exception e) {
        log.error(message, e);
        ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.SYSTEM_ERROR, message, e);
    }
}
