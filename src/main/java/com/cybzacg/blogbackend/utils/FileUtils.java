package com.cybzacg.blogbackend.utils;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文件操作工具类
 * 提供文件和目录的常用操作方法
 *
 * @author demo
 */
public class FileUtils {

    /**
     * 默认字符编码
     */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private FileUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== 文件/目录检查 ====================

    /**
     * 判断文件是否存在
     *
     * @param filePath 文件路径
     * @return true-存在，false-不存在
     */
    public static boolean exists(String filePath) {
        if (StrUtils.isEmpty(filePath)) {
            return false;
        }
        return new File(filePath).exists();
    }

    /**
     * 判断文件是否存在
     *
     * @param file 文件对象
     * @return true-存在，false-不存在
     */
    public static boolean exists(File file) {
        return file != null && file.exists();
    }

    /**
     * 判断是否为文件
     *
     * @param filePath 文件路径
     * @return true-是文件，false-不是文件
     */
    public static boolean isFile(String filePath) {
        return filePath != null && new File(filePath).isFile();
    }

    /**
     * 判断是否为目录
     *
     * @param dirPath 目录路径
     * @return true-是目录，false-不是目录
     */
    public static boolean isDirectory(String dirPath) {
        return dirPath != null && new File(dirPath).isDirectory();
    }

    // ==================== 文件/目录创建 ====================

    /**
     * 创建文件（包括父目录）
     *
     * @param filePath 文件路径
     * @return 创建的文件对象
     * @throws IOException 创建失败时抛出异常
     */
    public static File createFile(String filePath) throws IOException {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        File file = new File(filePath);
        if (!file.exists()) {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            file.createNewFile();
        }
        return file;
    }

    /**
     * 创建目录
     *
     * @param dirPath 目录路径
     * @return 创建的目录对象
     */
    public static File createDirectory(String dirPath) {
        Objects.requireNonNull(dirPath, "目录路径不能为空");
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    // ==================== 文件/目录删除 ====================

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return true-删除成功，false-删除失败
     */
    public static boolean deleteFile(String filePath) {
        if (StrUtils.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return file.exists() && file.isFile() && file.delete();
    }

    /**
     * 删除目录及其所有内容
     *
     * @param dirPath 目录路径
     * @return true-删除成功，false-删除失败
     */
    public static boolean deleteDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        return deleteDirContent(dir);
    }

    /**
     * 递归删除目录内容
     *
     * @param dir 目录对象
     * @return true-删除成功，false-删除失败
     */
    private static boolean deleteDirContent(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirContent(file);
                }
                file.delete();
            }
        }
        return dir.delete();
    }

    // ==================== 文件复制 ====================

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 复制的文件大小（字节）
     * @throws IOException IO异常
     */
    public static long copyFile(String sourcePath, String targetPath) throws IOException {
        return copyFile(new File(sourcePath), new File(targetPath));
    }

    /**
     * 复制文件
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @return 复制的文件大小（字节）
     * @throws IOException IO异常
     */
    public static long copyFile(File sourceFile, File targetFile) throws IOException {
        Objects.requireNonNull(sourceFile, "源文件不能为空");
        Objects.requireNonNull(targetFile, "目标文件不能为空");

        if (!sourceFile.exists()) {
            throw new FileNotFoundException("源文件不存在: " + sourceFile.getAbsolutePath());
        }

        // 创建目标文件的父目录
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(targetFile)) {
            return InputStreamUtils.copy(in, out);
        }
    }

    /**
     * 复制目录
     *
     * @param sourceDirPath 源目录路径
     * @param targetDirPath 目标目录路径
     * @throws IOException IO异常
     */
    public static void copyDirectory(String sourceDirPath, String targetDirPath) throws IOException {
        copyDirectory(new File(sourceDirPath), new File(targetDirPath));
    }

    /**
     * 复制目录
     *
     * @param sourceDir 源目录
     * @param targetDir 目标目录
     * @throws IOException IO异常
     */
    public static void copyDirectory(File sourceDir, File targetDir) throws IOException {
        Objects.requireNonNull(sourceDir, "源目录不能为空");
        Objects.requireNonNull(targetDir, "目标目录不能为空");

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new FileNotFoundException("源目录不存在或不是目录: " + sourceDir.getAbsolutePath());
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    copyDirectory(file, new File(targetDir, file.getName()));
                } else {
                    copyFile(file, new File(targetDir, file.getName()));
                }
            }
        }
    }

    // ==================== 文件移动/重命名 ====================

    /**
     * 移动文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return true-移动成功，false-移动失败
     */
    public static boolean moveFile(String sourcePath, String targetPath) {
        return moveFile(new File(sourcePath), new File(targetPath));
    }

    /**
     * 移动文件
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @return true-移动成功，false-移动失败
     */
    public static boolean moveFile(File sourceFile, File targetFile) {
        Objects.requireNonNull(sourceFile, "源文件不能为空");
        Objects.requireNonNull(targetFile, "目标文件不能为空");

        if (!sourceFile.exists()) {
            return false;
        }

        // 创建目标文件的父目录
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        return sourceFile.renameTo(targetFile);
    }

    // ==================== 文件读写 ====================

    /**
     * 读取文件内容为字符串
     *
     * @param filePath 文件路径
     * @return 文件内容字符串
     * @throws IOException IO异常
     */
    public static String readString(String filePath) throws IOException {
        return readString(filePath, DEFAULT_CHARSET);
    }

    /**
     * 读取文件内容为字符串
     *
     * @param filePath 文件路径
     * @param charset  字符编码
     * @return 文件内容字符串
     * @throws IOException IO异常
     */
    public static String readString(String filePath, Charset charset) throws IOException {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        return readString(new File(filePath), charset);
    }

    /**
     * 读取文件内容为字符串
     *
     * @param file    文件对象
     * @param charset 字符编码
     * @return 文件内容字符串
     * @throws IOException IO异常
     */
    public static String readString(File file, Charset charset) throws IOException {
        Objects.requireNonNull(file, "文件不能为空");
        try (InputStream in = new FileInputStream(file)) {
            return InputStreamUtils.toString(in, charset);
        }
    }

    /**
     * 读取文件内容为字节数组
     *
     * @param filePath 文件路径
     * @return 字节数组
     * @throws IOException IO异常
     */
    public static byte[] readBytes(String filePath) throws IOException {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        return readBytes(new File(filePath));
    }

    /**
     * 读取文件内容为字节数组
     *
     * @param file 文件对象
     * @return 字节数组
     * @throws IOException IO异常
     */
    public static byte[] readBytes(File file) throws IOException {
        Objects.requireNonNull(file, "文件不能为空");
        try (InputStream in = new FileInputStream(file)) {
            return InputStreamUtils.toByteArray(in);
        }
    }

    /**
     * 读取文件的所有行
     *
     * @param filePath 文件路径
     * @return 行列表
     * @throws IOException IO异常
     */
    public static List<String> readLines(String filePath) throws IOException {
        return readLines(filePath, DEFAULT_CHARSET);
    }

    /**
     * 读取文件的所有行
     *
     * @param filePath 文件路径
     * @param charset  字符编码
     * @return 行列表
     * @throws IOException IO异常
     */
    public static List<String> readLines(String filePath, Charset charset) throws IOException {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        try (InputStream in = new FileInputStream(filePath)) {
            return InputStreamUtils.readLines(in, charset);
        }
    }

    /**
     * 写入字符串到文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @throws IOException IO异常
     */
    public static void writeString(String filePath, String content) throws IOException {
        writeString(filePath, content, DEFAULT_CHARSET, false);
    }

    /**
     * 写入字符串到文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @param append   是否追加
     * @throws IOException IO异常
     */
    public static void writeString(String filePath, String content, boolean append) throws IOException {
        writeString(filePath, content, DEFAULT_CHARSET, append);
    }

    /**
     * 写入字符串到文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @param charset  字符编码
     * @param append   是否追加
     * @throws IOException IO异常
     */
    public static void writeString(String filePath, String content, Charset charset, boolean append) throws IOException {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        writeString(new File(filePath), content, charset, append);
    }

    /**
     * 写入字符串到文件
     *
     * @param file    文件对象
     * @param content 内容
     * @param charset 字符编码
     * @param append  是否追加
     * @throws IOException IO异常
     */
    public static void writeString(File file, String content, Charset charset, boolean append) throws IOException {
        Objects.requireNonNull(file, "文件不能为空");
        try (OutputStream out = new FileOutputStream(file, append)) {
            out.write(content.getBytes(charset));
        }
    }

    /**
     * 写入字节数组到文件
     *
     * @param filePath 文件路径
     * @param data     字节数组
     * @throws IOException IO异常
     */
    public static void writeBytes(String filePath, byte[] data) throws IOException {
        writeBytes(filePath, data, false);
    }

    /**
     * 写入字节数组到文件
     *
     * @param filePath 文件路径
     * @param data     字节数组
     * @param append   是否追加
     * @throws IOException IO异常
     */
    public static void writeBytes(String filePath, byte[] data, boolean append) throws IOException {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        Objects.requireNonNull(data, "数据不能为空");
        try (OutputStream out = new FileOutputStream(filePath, append)) {
            out.write(data);
        }
    }

    /**
     * 追加内容到文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @throws IOException IO异常
     */
    public static void append(String filePath, String content) throws IOException {
        writeString(filePath, content, DEFAULT_CHARSET, true);
    }

    /**
     * 追加内容到文件
     *
     * @param filePath 文件路径
     * @param content  内容
     * @param charset  字符编码
     * @throws IOException IO异常
     */
    public static void append(String filePath, String content, Charset charset) throws IOException {
        writeString(filePath, content, charset, true);
    }

    /**
     * 写入行到文件
     *
     * @param filePath 文件路径
     * @param lines    行列表
     * @throws IOException IO异常
     */
    public static void writeLines(String filePath, List<String> lines) throws IOException {
        writeLines(filePath, lines, DEFAULT_CHARSET, false);
    }

    /**
     * 写入行到文件
     *
     * @param filePath 文件路径
     * @param lines    行列表
     * @param append   是否追加
     * @throws IOException IO异常
     */
    public static void writeLines(String filePath, List<String> lines, boolean append) throws IOException {
        writeLines(filePath, lines, DEFAULT_CHARSET, append);
    }

    /**
     * 写入行到文件
     *
     * @param filePath 文件路径
     * @param lines    行列表
     * @param charset  字符编码
     * @param append   是否追加
     * @throws IOException IO异常
     */
    public static void writeLines(String filePath, List<String> lines, Charset charset, boolean append) throws IOException {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        Objects.requireNonNull(lines, "行列表不能为空");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath, append), charset))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // ==================== 文件信息获取 ====================

    /**
     * 获取文件扩展名（不包含点）
     *
     * @param filePath 文件路径
     * @return 文件扩展名，无扩展名返回空字符串
     */
    public static String getExtension(String filePath) {
        if (StrUtils.isEmpty(filePath)) {
            return "";
        }
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filePath.length() - 1) {
            return "";
        }
        return filePath.substring(lastDotIndex + 1);
    }

    /**
     * 获取文件扩展名（不包含点）
     *
     * @param file 文件对象
     * @return 文件扩展名，无扩展名返回空字符串
     */
    public static String getExtension(File file) {
        if (file == null) {
            return "";
        }
        return getExtension(file.getName());
    }

    /**
     * 获取文件名（不含路径）
     *
     * @param filePath 文件路径
     * @return 文件名
     */
    public static String getFileName(String filePath) {
        if (StrUtils.isEmpty(filePath)) {
            return "";
        }
        int lastSeparatorIndex = Math.max(
                filePath.lastIndexOf('/'),
                filePath.lastIndexOf('\\')
        );
        return lastSeparatorIndex == -1 ? filePath : filePath.substring(lastSeparatorIndex + 1);
    }

    /**
     * 获取文件名（不含路径和扩展名）
     *
     * @param filePath 文件路径
     * @return 文件名（不含扩展名）
     */
    public static String getBaseName(String filePath) {
        String fileName = getFileName(filePath);
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? fileName : fileName.substring(0, lastDotIndex);
    }

    /**
     * 获取文件大小（字节）
     *
     * @param filePath 文件路径
     * @return 文件大小（字节），文件不存在返回0
     */
    public static long getSize(String filePath) {
        if (StrUtils.isEmpty(filePath)) {
            return 0;
        }
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }

    /**
     * 获取格式化的文件大小
     *
     * @param filePath 文件路径
     * @return 格式化后的文件大小（如：1.23 KB）
     */
    public static String getFormattedSize(String filePath) {
        return formatSize(getSize(filePath));
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小
     */
    public static String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 获取目录大小
     *
     * @param dirPath 目录路径
     * @return 目录大小（字节）
     */
    public static long getDirectorySize(String dirPath) {
        return getDirectorySize(new File(dirPath));
    }

    /**
     * 获取目录大小
     *
     * @param dir 目录对象
     * @return 目录大小（字节）
     */
    public static long getDirectorySize(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += getDirectorySize(file);
                }
            }
        }
        return size;
    }

    // ==================== 文件列表获取 ====================

    /**
     * 获取目录下的所有文件
     *
     * @param dirPath 目录路径
     * @return 文件列表
     */
    public static List<File> listFiles(String dirPath) {
        return listFiles(dirPath, null);
    }

    /**
     * 获取目录下的所有文件（包含子目录）
     *
     * @param dirPath 目录路径
     * @return 文件列表
     */
    public static List<File> listAllFiles(String dirPath) {
        List<File> files = new ArrayList<>();
        listAllFiles(new File(dirPath), files);
        return files;
    }

    /**
     * 递归获取目录下的所有文件
     *
     * @param dir   目录对象
     * @param files 文件列表
     */
    private static void listAllFiles(File dir, List<File> files) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] subFiles = dir.listFiles();
        if (subFiles != null) {
            for (File file : subFiles) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    listAllFiles(file, files);
                }
            }
        }
    }

    /**
     * 获取目录下指定扩展名的文件
     *
     * @param dirPath    目录路径
     * @param extensions 扩展名数组（如：{"txt", "log"}）
     * @return 文件列表
     */
    public static List<File> listFiles(String dirPath, String[] extensions) {
        List<File> files = new ArrayList<>();
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return files;
        }
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null) {
            for (File file : dirFiles) {
                if (file.isFile()) {
                    if (extensions == null || extensions.length == 0) {
                        files.add(file);
                    } else {
                        String extension = getExtension(file);
                        for (String ext : extensions) {
                            if (ext.equalsIgnoreCase(extension)) {
                                files.add(file);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return files;
    }

    /**
     * 获取临时文件路径
     *
     * @param prefix 文件名前缀
     * @param suffix 文件名后缀
     * @return 临时文件对象
     * @throws IOException IO异常
     */
    public static File createTempFile(String prefix, String suffix) throws IOException {
        return File.createTempFile(prefix, suffix);
    }

    /**
     * 获取临时文件路径
     *
     * @param prefix  文件名前缀
     * @param suffix  文件名后缀
     * @param dirPath 临时目录路径
     * @return 临时文件对象
     * @throws IOException IO异常
     */
    public static File createTempFile(String prefix, String suffix, String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return File.createTempFile(prefix, suffix, dir);
    }

    /**
     * 获取系统临时目录
     *
     * @return 临时目录路径
     */
    public static String getTempDirPath() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * 获取用户主目录
     *
     * @return 用户主目录路径
     */
    public static String getUserHomePath() {
        return System.getProperty("user.home");
    }

    /**
     * 获取当前工作目录
     *
     * @return 当前工作目录路径
     */
    public static String getCurrentDirPath() {
        return System.getProperty("user.dir");
    }

    /**
     * 获取规范路径（去除.和..等）
     *
     * @param path 路径
     * @return 规范路径
     * @throws IOException IO异常
     */
    public static String getCanonicalPath(String path) throws IOException {
        return new File(path).getCanonicalPath();
    }

    /**
     * 获取绝对路径
     *
     * @param path 路径
     * @return 绝对路径
     */
    public static String getAbsolutePath(String path) {
        return new File(path).getAbsolutePath();
    }

    // ==================== 文件元数据标准化 ====================

    /**
     * 字节数组转十六进制字符串。
     */
    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /**
     * 标准化 MD5 字符串。
     *
     * @return 空或空白输入返回 null，否则返回 trim + lowercase 结果
     */
    public static String normalizeMd5(String md5) {
        if (!StrUtils.hasText(md5)) {
            return null;
        }
        return StrUtils.trimToLowerCase(md5);
    }

    /**
     * 标准化文件扩展名。
     *
     * @return null 输入返回 null，空或空白输入返回 ""，否则返回 trim + lowercase 结果
     */
    public static String normalizeExt(String ext) {
        if (ext == null) {
            return null;
        }
        if (!StrUtils.hasText(ext)) {
            return "";
        }
        return StrUtils.trimToLowerCase(ext);
    }

    /**
     * 截断字符串到最大长度。
     *
     * @return 空或空白输入原样返回，否则截断到 maxLen 字符
     */
    public static String truncate(String value, int maxLen) {
        if (!StrUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }
}

