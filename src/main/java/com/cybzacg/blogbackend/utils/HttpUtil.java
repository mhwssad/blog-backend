package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 请求工具类。
 *
 * <p>基于 OkHttp 提供同步请求、异步请求与文件下载能力，并统一请求构建、
 * 日志输出与异常抛出方式。</p>
 */
@Slf4j
public class HttpUtil {
    /**
     * JSON 媒体类型。
     */
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 二进制流媒体类型。
     */
    private static final MediaType STREAM = MediaType.parse("application/octet-stream");

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
     * 下载缓冲区大小。
     */
    private static final int DOWNLOAD_BUFFER_SIZE = 8192;

    /**
     * Range 请求头名称。
     */
    private static final String RANGE_HEADER = "Range";

    /**
     * 默认 OkHttpClient 实例。
     */
    private static final OkHttpClient OK_HTTP_CLIENT = createBaseClientBuilder(
            CONNECT_TIMEOUT, READ_TIMEOUT, WRITE_TIMEOUT).build();

    private HttpUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取默认的 OkHttpClient 实例。
     *
     * @return OkHttpClient 实例
     */
    public static OkHttpClient getOkHttpClient() {
        return OK_HTTP_CLIENT;
    }

    /**
     * 创建自定义超时的 OkHttpClient。
     *
     * @param connectTimeoutSeconds 连接超时时间（秒）
     * @param readTimeoutSeconds    读取超时时间（秒）
     * @return OkHttpClient 实例
     */
    public static OkHttpClient createCustomClient(long connectTimeoutSeconds, long readTimeoutSeconds) {
        return createBaseClientBuilder(connectTimeoutSeconds, readTimeoutSeconds, WRITE_TIMEOUT).build();
    }

    /**
     * GET 请求。
     *
     * @param url 请求 URL
     * @return 响应字符串
     */
    public static String get(String url) {
        return get(url, null);
    }

    /**
     * GET 请求（带请求头）。
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String get(String url, Map<String, String> headers) {
        return execute(buildGetRequest(url, null, headers));
    }

    /**
     * GET 请求（带参数和请求头）。
     *
     * @param url     请求 URL
     * @param params  查询参数
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String get(String url, Map<String, String> params, Map<String, String> headers) {
        return execute(buildGetRequest(url, params, headers));
    }

    /**
     * POST 请求（JSON 字符串）。
     *
     * @param url  请求 URL
     * @param json JSON 请求体
     * @return 响应字符串
     */
    public static String postJson(String url, String json) {
        return postJson(url, json, null);
    }

    /**
     * POST 请求（JSON 字符串，带请求头）。
     *
     * @param url     请求 URL
     * @param json    JSON 请求体
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String postJson(String url, String json, Map<String, String> headers) {
        return execute(buildRequest(url, "POST", createJsonBody(json), headers));
    }

    /**
     * POST 请求（对象自动序列化为 JSON）。
     *
     * @param url 请求 URL
     * @param obj 请求体对象
     * @return 响应字符串
     */
    public static String postJson(String url, Object obj) {
        return postJson(url, obj, null);
    }

    /**
     * POST 请求（对象自动序列化为 JSON，带请求头）。
     *
     * @param url     请求 URL
     * @param obj     请求体对象
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String postJson(String url, Object obj, Map<String, String> headers) {
        return postJson(url, JsonUtils.toJson(obj), headers);
    }

    /**
     * POST 请求（表单格式）。
     *
     * @param url      请求 URL
     * @param formData 表单数据
     * @return 响应字符串
     */
    public static String postForm(String url, Map<String, String> formData) {
        return postForm(url, formData, null);
    }

    /**
     * POST 请求（表单格式，带请求头）。
     *
     * @param url      请求 URL
     * @param formData 表单数据
     * @param headers  请求头
     * @return 响应字符串
     */
    public static String postForm(String url, Map<String, String> formData, Map<String, String> headers) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (formData != null && !formData.isEmpty()) {
            formData.forEach(formBuilder::add);
        }
        return execute(buildRequest(url, "POST", formBuilder.build(), headers));
    }

    /**
     * POST 请求（上传文件）。
     *
     * @param url       请求 URL
     * @param file      上传文件
     * @param paramName 文件参数名
     * @return 响应字符串
     */
    public static String uploadFile(String url, File file, String paramName) {
        return uploadFile(url, file, paramName, null);
    }

    /**
     * POST 请求（上传文件，带请求头）。
     *
     * @param url       请求 URL
     * @param file      上传文件
     * @param paramName 文件参数名
     * @param headers   请求头
     * @return 响应字符串
     */
    public static String uploadFile(String url, File file, String paramName, Map<String, String> headers) {
        return uploadFile(url, file, paramName, null, headers);
    }

    /**
     * POST 请求（上传文件并附带额外表单参数）。
     *
     * @param url       请求 URL
     * @param file      上传文件
     * @param paramName 文件参数名
     * @param formData  额外表单参数
     * @param headers   请求头
     * @return 响应字符串
     */
    public static String uploadFile(String url,
                                    File file,
                                    String paramName,
                                    Map<String, String> formData,
                                    Map<String, String> headers) {
        MultipartBody.Builder multipartBuilder = createMultipartBuilder(file, paramName, formData);
        return execute(buildRequest(url, "POST", multipartBuilder.build(), headers));
    }

    /**
     * PUT 请求（JSON 字符串）。
     *
     * @param url  请求 URL
     * @param json JSON 请求体
     * @return 响应字符串
     */
    public static String putJson(String url, String json) {
        return putJson(url, json, null);
    }

    /**
     * PUT 请求（JSON 字符串，带请求头）。
     *
     * @param url     请求 URL
     * @param json    JSON 请求体
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String putJson(String url, String json, Map<String, String> headers) {
        return execute(buildRequest(url, "PUT", createJsonBody(json), headers));
    }

    /**
     * PUT 请求（对象自动序列化为 JSON）。
     *
     * @param url 请求 URL
     * @param obj 请求体对象
     * @return 响应字符串
     */
    public static String putJson(String url, Object obj) {
        return putJson(url, obj, null);
    }

    /**
     * PUT 请求（对象自动序列化为 JSON，带请求头）。
     *
     * @param url     请求 URL
     * @param obj     请求体对象
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String putJson(String url, Object obj, Map<String, String> headers) {
        return putJson(url, JsonUtils.toJson(obj), headers);
    }

    /**
     * DELETE 请求。
     *
     * @param url 请求 URL
     * @return 响应字符串
     */
    public static String delete(String url) {
        return delete(url, null);
    }

    /**
     * DELETE 请求（带请求头）。
     *
     * @param url     请求 URL
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String delete(String url, Map<String, String> headers) {
        return execute(buildRequest(url, "DELETE", null, headers));
    }

    /**
     * DELETE 请求（带请求体）。
     *
     * @param url  请求 URL
     * @param json JSON 请求体
     * @return 响应字符串
     */
    public static String deleteWithBody(String url, String json) {
        return deleteWithBody(url, json, null);
    }

    /**
     * DELETE 请求（带请求体和请求头）。
     *
     * @param url     请求 URL
     * @param json    JSON 请求体
     * @param headers 请求头
     * @return 响应字符串
     */
    public static String deleteWithBody(String url, String json, Map<String, String> headers) {
        return execute(buildRequest(url, "DELETE", createJsonBody(json), headers));
    }

    /**
     * 同步执行请求并读取响应字符串。
     *
     * @param request 请求对象
     * @return 响应字符串
     */
    public static String execute(Request request) {
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            validateSuccessfulResponse(response, "HTTP请求", request.url().toString());
            return readResponseBodyAsString(response, "HTTP请求", request.url().toString());
        } catch (IOException e) {
            throwIoBusinessException("HTTP请求异常", request.url().toString(), e);
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * 同步执行请求，返回响应对象，供调用方自行处理响应内容。
     *
     * @param request 请求对象
     * @return 响应对象
     * @throws IOException IO 异常
     */
    public static Response executeReturnResponse(Request request) throws IOException {
        return OK_HTTP_CLIENT.newCall(request).execute();
    }

    /**
     * 异步执行请求。
     *
     * @param request  请求对象
     * @param callback 回调函数
     */
    public static void executeAsync(Request request, Callback callback) {
        OK_HTTP_CLIENT.newCall(request).enqueue(callback);
    }

    /**
     * 下载文件到指定路径。
     *
     * @param url      下载 URL
     * @param savePath 保存路径（含文件名）
     * @return 下载后的文件
     */
    public static File downloadFile(String url, String savePath) {
        return downloadFile(url, savePath, null);
    }

    /**
     * 下载文件到指定路径（带请求头）。
     *
     * @param url      下载 URL
     * @param savePath 保存路径（含文件名）
     * @param headers  请求头
     * @return 下载后的文件
     */
    public static File downloadFile(String url, String savePath, Map<String, String> headers) {
        return downloadFile(url, savePath, headers, 0);
    }

    /**
     * 下载文件到指定路径（支持断点续传）。
     *
     * @param url      下载 URL
     * @param savePath 保存路径（含文件名）
     * @param headers  请求头
     * @param offset   起始位置；大于 0 时要求服务端返回 206
     * @return 下载后的文件
     */
    public static File downloadFile(String url, String savePath, Map<String, String> headers, long offset) {
        Request request = buildDownloadRequest(url, headers, offset);
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            validateDownloadResponse(response, request.url().toString(), offset);
            ResponseBody responseBody = requireResponseBody(response, "文件下载", request.url().toString());
            File targetFile = prepareTargetFile(savePath);
            writeResponseBodyToFile(responseBody, targetFile, offset > 0);
            log.info("文件下载成功: {} (大小: {} bytes)", savePath, targetFile.length());
            return targetFile;
        } catch (IOException e) {
            throwIoBusinessException("文件下载异常", request.url().toString(), e);
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * 下载文件并返回字节数组。
     *
     * @param url 下载 URL
     * @return 文件字节数组
     */
    public static byte[] downloadFileAsBytes(String url) {
        return downloadFileAsBytes(url, null);
    }

    /**
     * 下载文件并返回字节数组（带请求头）。
     *
     * @param url     下载 URL
     * @param headers 请求头
     * @return 文件字节数组
     */
    public static byte[] downloadFileAsBytes(String url, Map<String, String> headers) {
        Request request = buildGetRequest(url, null, headers);
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            validateSuccessfulResponse(response, "文件下载", request.url().toString());
            byte[] bytes = requireResponseBody(response, "文件下载", request.url().toString()).bytes();
            log.info("文件下载成功: {} (大小: {} bytes)", request.url(), bytes.length);
            return bytes;
        } catch (IOException e) {
            throwIoBusinessException("文件下载异常", request.url().toString(), e);
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * 异步下载文件。
     *
     * @param url      下载 URL
     * @param savePath 保存路径
     * @param callback 回调函数
     */
    public static void downloadFileAsync(String url, String savePath, Callback callback) {
        downloadFileAsync(url, savePath, null, callback);
    }

    /**
     * 异步下载文件（带请求头）。
     *
     * @param url      下载 URL
     * @param savePath 保存路径
     * @param headers  请求头
     * @param callback 回调函数
     */
    public static void downloadFileAsync(String url, String savePath, Map<String, String> headers, Callback callback) {
        Request request = buildGetRequest(url, null, headers);
        OK_HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("异步文件下载失败: {}", request.url(), e);
                callback.onFailure(call, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    validateSuccessfulResponse(response, "异步文件下载", request.url().toString());
                    ResponseBody responseBody = requireResponseBody(response, "异步文件下载", request.url().toString());
                    File targetFile = prepareTargetFile(savePath);
                    writeResponseBodyToFile(responseBody, targetFile, false);
                    log.info("异步文件下载成功: {} (大小: {} bytes)", savePath, targetFile.length());
                    callback.onResponse(call, response);
                } catch (BusinessException e) {
                    log.error("异步文件下载失败: {}", request.url(), e);
                    callback.onFailure(call, new IOException(e.getMessage(), e));
                }
            }
        });
    }

    /**
     * 创建通用的 OkHttpClient.Builder，集中维护超时与重试策略。
     */
    private static OkHttpClient.Builder createBaseClientBuilder(long connectTimeoutSeconds,
                                                                long readTimeoutSeconds,
                                                                long writeTimeoutSeconds) {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
    }

    /**
     * 创建 GET 请求对象，统一处理 URL 参数与请求头。
     */
    private static Request buildGetRequest(String url, Map<String, String> params, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().get().url(buildHttpUrl(url, params));
        return applyHeaders(builder, headers).build();
    }

    /**
     * 创建通用请求对象，统一处理 HTTP 方法、URL 与请求头。
     */
    private static Request buildRequest(String url, String method, RequestBody body, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(buildHttpUrl(url, null)).method(method, body);
        return applyHeaders(builder, headers).build();
    }

    /**
     * 创建下载请求，并在断点续传场景下注入 Range 请求头。
     */
    private static Request buildDownloadRequest(String url, Map<String, String> headers, long offset) {
        Request.Builder builder = new Request.Builder().get().url(buildHttpUrl(url, null));
        applyHeaders(builder, headers);
        if (offset > 0) {
            builder.header(RANGE_HEADER, "bytes=" + offset + "-");
        }
        return builder.build();
    }

    /**
     * 将 Header Map 统一写入 Request.Builder。
     */
    private static Request.Builder applyHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::addHeader);
        }
        return builder;
    }

    /**
     * 构建带查询参数的 HttpUrl，并统一处理非法 URL 场景。
     */
    private static HttpUrl buildHttpUrl(String url, Map<String, String> params) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.ILLEGAL_ARGUMENT, "请求地址非法: " + url);
        }
        if (params == null || params.isEmpty()) {
            return httpUrl;
        }
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        params.forEach(urlBuilder::addQueryParameter);
        return urlBuilder.build();
    }

    /**
     * 构建 JSON 请求体，空字符串按空 JSON 处理，减少调用方判空分支。
     */
    private static RequestBody createJsonBody(String json) {
        return RequestBody.create(json == null ? "" : json, JSON);
    }

    /**
     * 构建文件上传的 multipart 请求体。
     */
    private static MultipartBody.Builder createMultipartBuilder(File file,
                                                                String paramName,
                                                                Map<String, String> formData) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(paramName, file.getName(), RequestBody.create(file, STREAM));
        if (formData != null && !formData.isEmpty()) {
            formData.forEach(multipartBuilder::addFormDataPart);
        }
        return multipartBuilder;
    }

    /**
     * 校验同步请求是否返回成功状态码。
     */
    private static void validateSuccessfulResponse(Response response, String action, String url) {
        if (response.isSuccessful()) {
            return;
        }
        String message = String.format("%s失败: %s -> %s %s", action, url, response.code(), response.message());
        log.error(message);
        throw new BusinessException(ResultErrorCode.FAIL.getCode(), message);
    }

    /**
     * 校验下载响应。断点续传时强制要求服务端返回 206，避免文件内容被错误追加。
     */
    private static void validateDownloadResponse(Response response, String url, long offset) {
        if (offset > 0 && response.code() != 206) {
            String message = String.format("文件断点下载失败: %s -> %s %s", url, response.code(), response.message());
            log.error(message);
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.FAIL, message);
        }
        validateSuccessfulResponse(response, "文件下载", url);
    }

    /**
     * 读取字符串响应体，并在响应体缺失时抛出统一异常。
     */
    private static String readResponseBodyAsString(Response response, String action, String url) throws IOException {
        ResponseBody responseBody = requireResponseBody(response, action, url);
        return responseBody.string();
    }

    /**
     * 保证响应体存在，避免调用处重复判空。
     */
    private static ResponseBody requireResponseBody(Response response, String action, String url) {
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            return responseBody;
        }
        String message = String.format("%s失败: %s 响应体为空", action, url);
        log.error(message);
        throw new BusinessException(ResultErrorCode.FAIL.getCode(), message);
    }

    /**
     * 准备文件下载目标路径，并确保父目录已存在。
     */
    private static File prepareTargetFile(String savePath) {
        File targetFile = new File(savePath);
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs() && !parentDir.exists()) {
            String message = "创建下载目录失败: " + parentDir.getAbsolutePath();
            log.error(message);
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.IO_ERROR, message);
        }
        return targetFile;
    }

    /**
     * 将响应体写入目标文件，支持覆盖写入与追加写入两种模式。
     */
    private static void writeResponseBodyToFile(ResponseBody responseBody, File targetFile, boolean append)
            throws IOException {
        try (InputStream inputStream = responseBody.byteStream()) {
            if (append) {
                try (FileOutputStream outputStream = new FileOutputStream(targetFile, true)) {
                    byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                return;
            }
            Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 抛出 IO 场景下的统一业务异常，便于接入现有异常处理体系。
     */
    private static void throwIoBusinessException(String action, String url, IOException e) {
        String message = action + ": " + url;
        log.error(message, e);
        ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.IO_ERROR, message, e);
    }
}
