package com.cybzacg.blogbackend.utils;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;

/**
 * Path工具类，提供Java NIO Path类的常用操作方法
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>Path的创建和转换</li>
 *   <li>Path信息的获取和分析</li>
 *   <li>Path的拼接和解析</li>
 *   <li>Path的规范化和比较</li>
 *   <li>Path与File、URI等对象的转换</li>
 * </ul>
 * <p>
 * 所有方法都进行了空值安全处理，避免NullPointerException
 *
 * @author demo
 */
public class PathUtils {

    private PathUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== Path 创建方法 ====================

    /**
     * 根据路径字符串创建Path对象
     *
     * @param path 路径字符串，可为null
     * @return Path对象，如果path为null或空则返回null
     * @throws InvalidPathException 如果路径字符串无效
     */
    public static Path create(String path) {
        if (StrUtils.isEmpty(path)) {
            return null;
        }
        return Paths.get(path);
    }

    /**
     * 根据URI创建Path对象
     *
     * @param uri URI对象，可为null
     * @return Path对象，如果uri为null则返回null
     * @throws IllegalArgumentException    如果URI参数没有指定方案
     * @throws FileSystemNotFoundException 如果URI标识的文件系统不存在
     * @throws SecurityException           如果存在安全管理器且拒绝访问文件系统
     */
    public static Path create(URI uri) {
        if (uri == null) {
            return null;
        }
        return Paths.get(uri);
    }

    /**
     * 根据路径字符串序列创建Path对象
     *
     * @param first 路径的第一部分，可为null
     * @param more  路径的其余部分，可为null
     * @return Path对象，如果所有参数都为null则返回null
     * @throws InvalidPathException 如果路径字符串无效
     */
    public static Path create(String first, String... more) {
        if (first == null && (more == null || more.length == 0)) {
            return null;
        }
        return Paths.get(first, more);
    }

    /**
     * 根据URI字符串创建Path对象
     *
     * @param uriString URI字符串，可为null
     * @return Path对象，如果uriString为null或空则返回null
     * @throws URISyntaxException          如果URI字符串语法错误
     * @throws IllegalArgumentException    如果URI参数没有指定方案
     * @throws FileSystemNotFoundException 如果URI标识的文件系统不存在
     * @throws SecurityException           如果存在安全管理器且拒绝访问文件系统
     */
    public static Path fromUriString(String uriString) throws URISyntaxException {
        if (StrUtils.isEmpty(uriString)) {
            return null;
        }
        URI uri = new URI(uriString);
        return Paths.get(uri);
    }

    // ==================== Path 信息获取方法 ====================

    /**
     * 获取Path的文件名部分
     *
     * @param path Path对象，可为null
     * @return 文件名Path，如果path为null或没有文件名则返回null
     */
    public static Path getFileName(Path path) {
        return path == null ? null : path.getFileName();
    }

    /**
     * 获取Path的父路径
     *
     * @param path Path对象，可为null
     * @return 父路径Path，如果path为null或没有父路径则返回null
     */
    public static Path getParent(Path path) {
        return path == null ? null : path.getParent();
    }

    /**
     * 获取Path的根路径
     *
     * @param path Path对象，可为null
     * @return 根路径Path，如果path为null或没有根路径则返回null
     */
    public static Path getRoot(Path path) {
        return path == null ? null : path.getRoot();
    }

    /**
     * 获取Path的名称元素数量
     *
     * @param path Path对象，可为null
     * @return 名称元素数量，如果path为null则返回0
     */
    public static int getNameCount(Path path) {
        return path == null ? 0 : path.getNameCount();
    }

    /**
     * 获取Path中指定索引的名称元素
     *
     * @param path  Path对象，可为null
     * @param index 索引位置
     * @return 名称元素Path，如果path为null或索引无效则返回null
     */
    public static Path getName(Path path, int index) {
        if (path == null) {
            return null;
        }
        try {
            return path.getName(index);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取Path的子路径
     *
     * @param path  Path对象，可为null
     * @param begin 开始索引（包含）
     * @param end   结束索引（不包含）
     * @return 子路径Path，如果参数无效则返回null
     */
    public static Path subpath(Path path, int begin, int end) {
        if (path == null) {
            return null;
        }
        try {
            return path.subpath(begin, end);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 判断Path是否为绝对路径
     *
     * @param path Path对象，可为null
     * @return true-是绝对路径，false-不是绝对路径或path为null
     */
    public static boolean isAbsolute(Path path) {
        return path != null && path.isAbsolute();
    }

    /**
     * 判断Path是否以指定路径开头
     *
     * @param path  Path对象，可为null
     * @param other 要比较的Path，可为null
     * @return true-以指定路径开头，false-不以指定路径开头或任一参数为null
     */
    public static boolean startsWith(Path path, Path other) {
        return path != null && other != null && path.startsWith(other);
    }

    /**
     * 判断Path是否以指定路径字符串开头
     *
     * @param path  Path对象，可为null
     * @param other 要比较的路径字符串，可为null
     * @return true-以指定路径开头，false-不以指定路径开头或任一参数为null
     */
    public static boolean startsWith(Path path, String other) {
        if (path == null || other == null) {
            return false;
        }
        try {
            return path.startsWith(other);
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /**
     * 判断Path是否以指定路径结尾
     *
     * @param path  Path对象，可为null
     * @param other 要比较的Path，可为null
     * @return true-以指定路径结尾，false-不以指定路径结尾或任一参数为null
     */
    public static boolean endsWith(Path path, Path other) {
        return path != null && other != null && path.endsWith(other);
    }

    /**
     * 判断Path是否以指定路径字符串结尾
     *
     * @param path  Path对象，可为null
     * @param other 要比较的路径字符串，可为null
     * @return true-以指定路径结尾，false-不以指定路径结尾或任一参数为null
     */
    public static boolean endsWith(Path path, String other) {
        if (path == null || other == null) {
            return false;
        }
        try {
            return path.endsWith(other);
        } catch (InvalidPathException e) {
            return false;
        }
    }

    // ==================== Path 规范化方法 ====================

    /**
     * 规范化Path，去除冗余的名称元素
     *
     * @param path Path对象，可为null
     * @return 规范化后的Path，如果path为null则返回null
     */
    public static Path normalize(Path path) {
        return path == null ? null : path.normalize();
    }

    /**
     * 获取Path的绝对路径
     *
     * @param path Path对象，可为null
     * @return 绝对路径Path，如果path为null则返回null
     * @throws SecurityException 如果存在安全管理器且拒绝访问文件系统
     */
    public static Path toAbsolutePath(Path path) {
        return path == null ? null : path.toAbsolutePath();
    }

    /**
     * 获取Path的真实路径
     *
     * @param path Path对象，可为null
     * @return 真实路径Path，如果path为null则返回null
     * @throws IOException       如果发生I/O错误
     * @throws SecurityException 如果存在安全管理器且拒绝访问文件系统
     */
    public static Path toRealPath(Path path) throws IOException {
        return path == null ? null : path.toRealPath();
    }

    /**
     * 获取Path的URI表示
     *
     * @param path Path对象，可为null
     * @return URI对象，如果path为null则返回null
     * @throws SecurityException 如果存在安全管理器且拒绝访问文件系统
     * @throws IOError           如果发生I/O错误（Java 8及以上版本）
     */
    public static URI toUri(Path path) {
        return path == null ? null : path.toUri();
    }

    /**
     * 将Path转换为File对象
     *
     * @param path Path对象，可为null
     * @return File对象，如果path为null则返回null
     */
    public static File toFile(Path path) {
        return path == null ? null : path.toFile();
    }

    // ==================== Path 拼接方法 ====================

    /**
     * 拼接路径
     *
     * @param path  原始Path，可为null
     * @param other 要拼接的Path，可为null
     * @return 拼接后的Path，如果任一参数为null则返回非null参数
     */
    public static Path resolve(Path path, Path other) {
        if (path == null) {
            return other;
        }
        if (other == null) {
            return path;
        }
        return path.resolve(other);
    }

    /**
     * 拼接路径字符串
     *
     * @param path  原始Path，可为null
     * @param other 要拼接的路径字符串，可为null
     * @return 拼接后的Path，如果任一参数为null则返回非null参数
     */
    public static Path resolve(Path path, String other) {
        if (path == null) {
            return other == null ? null : Paths.get(other);
        }
        if (other == null) {
            return path;
        }
        return path.resolve(other);
    }

    /**
     * 拼接多个路径字符串
     *
     * @param path  原始Path，可为null
     * @param paths 要拼接的路径字符串数组，可为null
     * @return 拼接后的Path，如果所有参数都为null则返回null
     */
    public static Path resolve(Path path, String... paths) {
        if (path == null && (paths == null || paths.length == 0)) {
            return null;
        }
        if (path == null) {
            return create(paths[0], Arrays.copyOfRange(paths, 1, paths.length));
        }
        if (paths == null || paths.length == 0) {
            return path;
        }

        Path result = path;
        for (String p : paths) {
            if (p != null) {
                result = result.resolve(p);
            }
        }
        return result;
    }

    /**
     * 解析路径中的相对部分
     *
     * @param path  原始Path，可为null
     * @param other 要解析的Path，可为null
     * @return 解析后的Path，如果任一参数为null则返回null
     */
    public static Path resolveSibling(Path path, Path other) {
        if (path == null || other == null) {
            return null;
        }
        return path.resolveSibling(other);
    }

    /**
     * 解析路径中的相对部分（字符串）
     *
     * @param path  原始Path，可为null
     * @param other 要解析的路径字符串，可为null
     * @return 解析后的Path，如果任一参数为null则返回null
     */
    public static Path resolveSibling(Path path, String other) {
        if (path == null || other == null) {
            return null;
        }
        return path.resolveSibling(other);
    }

    /**
     * 获取相对于另一个路径的相对路径
     *
     * @param path  原始Path，可为null
     * @param other 参考Path，可为null
     * @return 相对路径Path，如果任一参数为null则返回null
     * @throws IllegalArgumentException 如果路径无法相对化
     */
    public static Path relativize(Path path, Path other) {
        if (path == null || other == null) {
            return null;
        }
        try {
            return path.relativize(other);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ==================== Path 比较方法 ====================

    /**
     * 比较两个Path的字典顺序
     *
     * @param path1 第一个Path，可为null
     * @param path2 第二个Path，可为null
     * @return 比较结果：0-相等，负数-path1小于path2，正数-path1大于path2
     */
    public static int compareTo(Path path1, Path path2) {
        if (path1 == null && path2 == null) {
            return 0;
        }
        if (path1 == null) {
            return -1;
        }
        if (path2 == null) {
            return 1;
        }
        return path1.compareTo(path2);
    }

    /**
     * 判断两个Path是否相等
     *
     * @param path1 第一个Path，可为null
     * @param path2 第二个Path，可为null
     * @return true-相等，false-不相等
     */
    public static boolean equals(Path path1, Path path2) {
        return Objects.equals(path1, path2);
    }

    /**
     * 判断两个Path是否指向同一个文件
     *
     * @param path1 第一个Path，可为null
     * @param path2 第二个Path，可为null
     * @return true-指向同一文件，false-不指向同一文件或任一参数为null
     * @throws IOException       如果发生I/O错误
     * @throws SecurityException 如果存在安全管理器且拒绝访问文件系统
     */
    public static boolean isSameFile(Path path1, Path path2) throws IOException {
        if (path1 == null || path2 == null) {
            return false;
        }
        return Files.isSameFile(path1, path2);
    }

    // ==================== Path 转换方法 ====================

    /**
     * 将Path转换为字符串
     *
     * @param path Path对象，可为null
     * @return 路径字符串，如果path为null则返回null
     */
    public static String toString(Path path) {
        return path == null ? null : path.toString();
    }

    /**
     * 将Path转换为字符串数组（分割所有名称元素）
     *
     * @param path Path对象，可为null
     * @return 名称元素字符串数组，如果path为null则返回空数组
     */
    public static String[] toStringArray(Path path) {
        if (path == null) {
            return new String[0];
        }

        List<String> names = new ArrayList<>();
        for (int i = 0; i < path.getNameCount(); i++) {
            names.add(path.getName(i).toString());
        }
        return names.toArray(new String[0]);
    }

    /**
     * 将Path转换为List（包含所有名称元素）
     *
     * @param path Path对象，可为null
     * @return 名称元素List，如果path为null则返回空List
     */
    public static List<String> toStringList(Path path) {
        if (path == null) {
            return new ArrayList<>();
        }

        List<String> names = new ArrayList<>();
        for (int i = 0; i < path.getNameCount(); i++) {
            names.add(path.getName(i).toString());
        }
        return names;
    }

    /**
     * 获取Path的迭代器
     *
     * @param path Path对象，可为null
     * @return 名称元素迭代器，如果path为null则返回空迭代器
     */
    public static Iterator<Path> iterator(Path path) {
        if (path == null) {
            return new ArrayList<Path>().iterator();
        }
        return path.iterator();
    }

    // ==================== 工具方法 ====================

    /**
     * 获取文件扩展名（不包含点）
     *
     * @param path Path对象，可为null
     * @return 文件扩展名，无扩展名或path为null则返回空字符串
     */
    public static String getExtension(Path path) {
        if (path == null) {
            return "";
        }
        Path fileName = path.getFileName();
        if (fileName == null) {
            return "";
        }
        String fileNameStr = fileName.toString();
        int lastDotIndex = fileNameStr.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileNameStr.length() - 1) {
            return "";
        }
        return fileNameStr.substring(lastDotIndex + 1);
    }

    /**
     * 获取文件名（不含路径）
     *
     * @param path Path对象，可为null
     * @return 文件名字符串，path为null则返回空字符串
     */
    public static String getFileNameString(Path path) {
        if (path == null) {
            return "";
        }
        Path fileName = path.getFileName();
        return fileName == null ? "" : fileName.toString();
    }

    /**
     * 获取文件名（不含路径和扩展名）
     *
     * @param path Path对象，可为null
     * @return 文件名（不含扩展名），path为null则返回空字符串
     */
    public static String getBaseName(Path path) {
        String fileName = getFileNameString(path);
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? fileName : fileName.substring(0, lastDotIndex);
    }

    /**
     * 获取当前工作目录的Path
     *
     * @return 当前工作目录的Path
     */
    public static Path getCurrentDirectory() {
        return Paths.get("").toAbsolutePath();
    }

    /**
     * 获取用户主目录的Path
     *
     * @return 用户主目录的Path
     */
    public static Path getUserHomeDirectory() {
        return Paths.get(System.getProperty("user.home"));
    }

    /**
     * 获取临时目录的Path
     *
     * @return 临时目录的Path
     */
    public static Path getTempDirectory() {
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    /**
     * 获取系统默认文件系统的Path分隔符
     *
     * @return Path分隔符字符串
     */
    public static String getSeparator() {
        return FileSystems.getDefault().getSeparator();
    }

    /**
     * 创建临时文件Path
     *
     * @param prefix 文件名前缀，可为null
     * @param suffix 文件名后缀，可为null
     * @return 临时文件Path，如果创建失败则返回null
     * @throws IllegalArgumentException      如果前缀少于3个字符
     * @throws UnsupportedOperationException 如果不支持创建临时文件
     * @throws SecurityException             如果存在安全管理器且拒绝访问文件系统
     * @throws IOException                   如果发生I/O错误
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        try {
            return Files.createTempFile(prefix, suffix);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 在指定目录创建临时文件Path
     *
     * @param dir    目录Path，可为null
     * @param prefix 文件名前缀，可为null
     * @param suffix 文件名后缀，可为null
     * @return 临时文件Path，如果创建失败则返回null
     * @throws IllegalArgumentException      如果前缀少于3个字符
     * @throws UnsupportedOperationException 如果不支持创建临时文件
     * @throws SecurityException             如果存在安全管理器且拒绝访问文件系统
     * @throws IOException                   如果发生I/O错误
     */
    public static Path createTempFile(Path dir, String prefix, String suffix) throws IOException {
        try {
            return dir == null ? Files.createTempFile(prefix, suffix)
                    : Files.createTempFile(dir, prefix, suffix);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建临时目录Path
     *
     * @param prefix 目录名前缀，可为null
     * @return 临时目录Path，如果创建失败则返回null
     * @throws IllegalArgumentException      如果前缀少于3个字符
     * @throws UnsupportedOperationException 如果不支持创建临时目录
     * @throws SecurityException             如果存在安全管理器且拒绝访问文件系统
     * @throws IOException                   如果发生I/O错误
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        try {
            return Files.createTempDirectory(prefix);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 在指定目录创建临时目录Path
     *
     * @param dir    目录Path，可为null
     * @param prefix 目录名前缀，可为null
     * @return 临时目录Path，如果创建失败则返回null
     * @throws IllegalArgumentException      如果前缀少于3个字符
     * @throws UnsupportedOperationException 如果不支持创建临时目录
     * @throws SecurityException             如果存在安全管理器且拒绝访问文件系统
     * @throws IOException                   如果发生I/O错误
     */
    public static Path createTempDirectory(Path dir, String prefix) throws IOException {
        try {
            return dir == null ? Files.createTempDirectory(prefix)
                    : Files.createTempDirectory(dir, prefix);
        } catch (Exception e) {
            return null;
        }
    }
}

