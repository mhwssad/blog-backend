package com.cybzacg.blogbackend.utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * InputStream 工具类
 * 提供输入流的常用操作方法，包括读取、转换、复制等功能
 *
 * @author demo
 */
public class InputStreamUtils {

    /**
     * 默认缓冲区大小：8KB
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * 默认字符编码
     */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private InputStreamUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== InputStream 读取为字节数组 ====================

    /**
     * 将 InputStream 读取为字节数组
     *
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO异常
     */
    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            copy(inputStream, outputStream, DEFAULT_BUFFER_SIZE);
            return outputStream.toByteArray();
        }
    }

    /**
     * 将 InputStream 读取为字节数组（指定缓冲区大小）
     *
     * @param inputStream 输入流
     * @param bufferSize  缓冲区大小
     * @return 字节数组
     * @throws IOException IO异常
     */
    public static byte[] toByteArrayWithBufferSize(InputStream inputStream, int bufferSize) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("缓冲区大小必须大于0");
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            copy(inputStream, outputStream, bufferSize);
            return outputStream.toByteArray();
        }
    }

    /**
     * 将 InputStream 读取为字节数组（指定读取长度）
     *
     * @param inputStream 输入流
     * @param length      要读取的长度
     * @return 字节数组
     * @throws IOException IO异常
     */
    public static byte[] readNBytes(InputStream inputStream, int length) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        if (length < 0) {
            throw new IllegalArgumentException("读取长度不能为负数");
        }
        byte[] buffer = new byte[length];
        int offset = 0;
        int remaining = length;
        while (remaining > 0) {
            int read = inputStream.read(buffer, offset, remaining);
            if (read == -1) {
                break;
            }
            offset += read;
            remaining -= read;
        }
        if (offset < length) {
            byte[] result = new byte[offset];
            System.arraycopy(buffer, 0, result, 0, offset);
            return result;
        }
        return buffer;
    }

    // ==================== InputStream 读取为字符串 ====================

    /**
     * 将 InputStream 读取为字符串（使用默认字符编码 UTF-8）
     *
     * @param inputStream 输入流
     * @return 字符串
     * @throws IOException IO异常
     */
    public static String toString(InputStream inputStream) throws IOException {
        return toString(inputStream, DEFAULT_CHARSET);
    }

    /**
     * 将 InputStream 读取为字符串
     *
     * @param inputStream 输入流
     * @param charset     字符编码
     * @return 字符串
     * @throws IOException IO异常
     */
    public static String toString(InputStream inputStream, Charset charset) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(charset, "字符编码不能为空");
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            copy(inputStream, outputStream, DEFAULT_BUFFER_SIZE);
            return outputStream.toString(charset.name());
        }
    }

    /**
     * 将 InputStream 读取为字符串
     *
     * @param inputStream 输入流
     * @param charsetName 字符编码名称
     * @return 字符串
     * @throws IOException IO异常
     */
    public static String toString(InputStream inputStream, String charsetName) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(charsetName, "字符编码不能为空");
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            copy(inputStream, outputStream, DEFAULT_BUFFER_SIZE);
            return outputStream.toString(charsetName);
        }
    }

    // ==================== InputStream 读取为行列表 ====================

    /**
     * 将 InputStream 读取为行列表（使用默认字符编码 UTF-8）
     *
     * @param inputStream 输入流
     * @return 行列表
     * @throws IOException IO异常
     */
    public static List<String> readLines(InputStream inputStream) throws IOException {
        return readLines(inputStream, DEFAULT_CHARSET);
    }

    /**
     * 将 InputStream 读取为行列表
     *
     * @param inputStream 输入流
     * @param charset     字符编码
     * @return 行列表
     * @throws IOException IO异常
     */
    public static List<String> readLines(InputStream inputStream, Charset charset) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(charset, "字符编码不能为空");
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    // ==================== InputStream 复制到 OutputStream ====================

    /**
     * 将 InputStream 复制到 OutputStream（使用默认缓冲区大小）
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @return 复制的字节数
     * @throws IOException IO异常
     */
    public static long copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        return copy(inputStream, outputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * 将 InputStream 复制到 OutputStream
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @param bufferSize   缓冲区大小
     * @return 复制的字节数
     * @throws IOException IO异常
     */
    public static long copy(InputStream inputStream, OutputStream outputStream, int bufferSize) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(outputStream, "输出流不能为空");
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("缓冲区大小必须大于0");
        }
        byte[] buffer = new byte[bufferSize];
        long total = 0;
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            total += bytesRead;
        }
        return total;
    }

    // ==================== InputStream 保存为文件 ====================

    /**
     * 将 InputStream 保存为文件
     *
     * @param inputStream 输入流
     * @param filePath    文件路径
     * @return 保存的字节数
     * @throws IOException IO异常
     */
    public static long toFile(InputStream inputStream, String filePath) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(filePath, "文件路径不能为空");
        return toFile(inputStream, new File(filePath));
    }

    /**
     * 将 InputStream 保存为文件
     *
     * @param inputStream 输入流
     * @param file        文件对象
     * @return 保存的字节数
     * @throws IOException IO异常
     */
    public static long toFile(InputStream inputStream, File file) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(file, "文件对象不能为空");

        // 创建父目录
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            return copy(inputStream, outputStream, DEFAULT_BUFFER_SIZE);
        }
    }

    // ==================== InputStream 跳过字节 ====================

    /**
     * 跳过 InputStream 中指定字节数
     *
     * @param inputStream 输入流
     * @param n           要跳过的字节数
     * @return 实际跳过的字节数
     * @throws IOException IO异常
     */
    public static long skip(InputStream inputStream, long n) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        if (n <= 0) {
            return 0;
        }

        long remaining = n;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        while (remaining > 0) {
            int bytesToSkip = (int) Math.min(buffer.length, remaining);
            int read = inputStream.read(buffer, 0, bytesToSkip);
            if (read == -1) {
                break;
            }
            remaining -= read;
        }

        return n - remaining;
    }

    // ==================== InputStream 读取可用字节 ====================

    /**
     * 读取 InputStream 中所有可用字节
     *
     * @param inputStream 输入流
     * @return 可用的字节数组
     * @throws IOException IO异常
     */
    public static byte[] readAvailable(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        int available = inputStream.available();
        if (available <= 0) {
            return new byte[0];
        }
        byte[] buffer = new byte[available];
        int bytesRead = inputStream.read(buffer);
        if (bytesRead == -1) {
            return new byte[0];
        }
        if (bytesRead < available) {
            byte[] result = new byte[bytesRead];
            System.arraycopy(buffer, 0, result, 0, bytesRead);
            return result;
        }
        return buffer;
    }

    // ==================== InputStream 包装器 ====================

    /**
     * 创建可重置的 BufferedInputStream
     * 支持通过 mark/reset 操作重复读取流内容
     *
     * @param inputStream 输入流
     * @return BufferedInputStream
     */
    public static BufferedInputStream buffered(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        return new BufferedInputStream(inputStream);
    }

    /**
     * 创建带缓冲大小限制的 BufferedInputStream
     *
     * @param inputStream 输入流
     * @param bufferSize  缓冲区大小
     * @return BufferedInputStream
     */
    public static BufferedInputStream buffered(InputStream inputStream, int bufferSize) {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("缓冲区大小必须大于0");
        }
        return new BufferedInputStream(inputStream, bufferSize);
    }

    /**
     * 创建数据输入流
     *
     * @param inputStream 输入流
     * @return DataInputStream
     */
    public static DataInputStream data(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        return new DataInputStream(inputStream);
    }

    /**
     * 创建带字符编码的输入流读取器
     *
     * @param inputStream 输入流
     * @param charset     字符编码
     * @return InputStreamReader
     */
    public static InputStreamReader reader(InputStream inputStream, Charset charset) {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(charset, "字符编码不能为空");
        return new InputStreamReader(inputStream, charset);
    }

    /**
     * 创建带字符编码的输入流读取器
     *
     * @param inputStream 输入流
     * @param charsetName 字符编码名称
     * @return InputStreamReader
     * @throws IllegalArgumentException 如果字符编码不支持
     */
    public static InputStreamReader reader(InputStream inputStream, String charsetName) {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(charsetName, "字符编码不能为空");
        try {
            return new InputStreamReader(inputStream, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("不支持的字符编码: " + charsetName, e);
        }
    }

    /**
     * 创建缓冲读取器（使用默认字符编码 UTF-8）
     *
     * @param inputStream 输入流
     * @return BufferedReader
     */
    public static BufferedReader bufferedReader(InputStream inputStream) {
        return bufferedReader(inputStream, DEFAULT_CHARSET);
    }

    /**
     * 创建缓冲读取器
     *
     * @param inputStream 输入流
     * @param charset     字符编码
     * @return BufferedReader
     */
    public static BufferedReader bufferedReader(InputStream inputStream, Charset charset) {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(charset, "字符编码不能为空");
        return new BufferedReader(new InputStreamReader(inputStream, charset));
    }

    /**
     * 创建缓冲读取器
     *
     * @param inputStream 输入流
     * @param charsetName 字符编码名称
     * @return BufferedReader
     * @throws IllegalArgumentException 如果字符编码不支持
     */
    public static BufferedReader bufferedReader(InputStream inputStream, String charsetName) {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        Objects.requireNonNull(charsetName, "字符编码不能为空");
        try {
            return new BufferedReader(new InputStreamReader(inputStream, charsetName));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("不支持的字符编码: " + charsetName, e);
        }
    }

    // ==================== InputStream 内容预览 ====================

    /**
     * 读取 InputStream 的前 N 个字节（用于内容预览）
     *
     * @param inputStream 输入流
     * @param length      要读取的长度
     * @return 前N个字节的字节数组
     * @throws IOException IO异常
     */
    public static byte[] preview(InputStream inputStream, int length) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        if (length <= 0) {
            throw new IllegalArgumentException("预览长度必须大于0");
        }
        return readNBytes(inputStream, length);
    }

    /**
     * 读取 InputStream 的前 N 个字节并转换为字符串（用于文本内容预览）
     *
     * @param inputStream 输入流
     * @param length      要读取的长度
     * @return 前N个字节的字符串
     * @throws IOException IO异常
     */
    public static String previewString(InputStream inputStream, int length) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        if (length <= 0) {
            throw new IllegalArgumentException("预览长度必须大于0");
        }
        byte[] bytes = readNBytes(inputStream, length);
        return new String(bytes, DEFAULT_CHARSET);
    }

    // ==================== InputStream 内容检测 ====================

    /**
     * 检测 InputStream 是否为空（没有可读数据）
     *
     * @param inputStream 输入流
     * @return true-为空，false-不为空
     * @throws IOException IO异常
     */
    public static boolean isEmpty(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        if (inputStream.available() == 0) {
            return true;
        }
        int data = inputStream.read();
        if (data == -1) {
            return true;
        }
        // 读取了一个字节，需要将其放回（如果支持）
        if (inputStream.markSupported()) {
            inputStream.reset();
            inputStream.mark(DEFAULT_BUFFER_SIZE);
        }
        return false;
    }

    /**
     * 获取 InputStream 中剩余可读的字节数
     *
     * @param inputStream 输入流
     * @return 剩余可读的字节数
     * @throws IOException IO异常
     */
    public static int getAvailable(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        return inputStream.available();
    }

    // ==================== InputStream 安全关闭 ====================

    /**
     * 安全关闭 InputStream，忽略异常
     *
     * @param inputStream 输入流
     */
    public static void closeQuietly(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ignored) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 安全关闭 InputStream，记录异常日志
     *
     * @param inputStream 输入流
     */
    public static void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // 记录异常日志
                System.err.println("关闭输入流时发生异常: " + e.getMessage());
            }
        }
    }

    // ==================== ByteArrayInputStream 工具方法 ====================

    /**
     * 从字节数组创建 ByteArrayInputStream
     *
     * @param bytes 字节数组
     * @return ByteArrayInputStream
     */
    public static ByteArrayInputStream fromByteArray(byte[] bytes) {
        Objects.requireNonNull(bytes, "字节数组不能为空");
        return new ByteArrayInputStream(bytes);
    }

    /**
     * 从字节数组创建 ByteArrayInputStream（支持偏移量和长度）
     *
     * @param bytes  字节数组
     * @param offset 偏移量
     * @param length 长度
     * @return ByteArrayInputStream
     */
    public static ByteArrayInputStream fromByteArray(byte[] bytes, int offset, int length) {
        Objects.requireNonNull(bytes, "字节数组不能为空");
        return new ByteArrayInputStream(bytes, offset, length);
    }

    /**
     * 从字符串创建 ByteArrayInputStream（使用默认字符编码 UTF-8）
     *
     * @param str 字符串
     * @return ByteArrayInputStream
     */
    public static ByteArrayInputStream fromString(String str) {
        return fromString(str, DEFAULT_CHARSET);
    }

    /**
     * 从字符串创建 ByteArrayInputStream
     *
     * @param str     字符串
     * @param charset 字符编码
     * @return ByteArrayInputStream
     */
    public static ByteArrayInputStream fromString(String str, Charset charset) {
        Objects.requireNonNull(str, "字符串不能为空");
        Objects.requireNonNull(charset, "字符编码不能为空");
        return new ByteArrayInputStream(str.getBytes(charset));
    }

    /**
     * 从字符串创建 ByteArrayInputStream
     *
     * @param str         字符串
     * @param charsetName 字符编码名称
     * @return ByteArrayInputStream
     * @throws IllegalArgumentException 如果字符编码不支持
     */
    public static ByteArrayInputStream fromString(String str, String charsetName) {
        Objects.requireNonNull(str, "字符串不能为空");
        Objects.requireNonNull(charsetName, "字符编码不能为空");
        try {
            return new ByteArrayInputStream(str.getBytes(charsetName));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("不支持的字符编码: " + charsetName, e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断 InputStream 是否支持 mark/reset
     *
     * @param inputStream 输入流
     * @return true-支持，false-不支持
     */
    public static boolean isMarkSupported(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        return inputStream.markSupported();
    }

    /**
     * 在 InputStream 上设置标记
     *
     * @param inputStream 输入流
     * @param readlimit   读取限制
     * @throws IOException 如果流不支持 mark 操作则抛出异常
     */
    public static void mark(InputStream inputStream, int readlimit) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        if (!inputStream.markSupported()) {
            throw new IOException("该输入流不支持 mark 操作");
        }
        inputStream.mark(readlimit);
    }

    /**
     * 将 InputStream 重置到标记位置
     *
     * @param inputStream 输入流
     * @throws IOException 如果流不支持 reset 操作则抛出异常
     */
    public static void reset(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "输入流不能为空");
        if (!inputStream.markSupported()) {
            throw new IOException("该输入流不支持 reset 操作");
        }
        inputStream.reset();
    }
}

