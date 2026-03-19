package com.cybzacg.blogbackend.utils;

import com.cybzacg.blogbackend.common.constant.HttpHeaderConstants;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * IP工具类
 * <p>
 * 获取客户端IP地址和IP地址对应的地理位置信息
 * <p>
 * 使用Nginx等反向代理软件， 则不能通过request.getRemoteAddr()获取IP地址
 * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，X-Forwarded-For中第一个非unknown的有效IP字符串，则为真实IP地址
 * </p>
 *
 * @author Ray
 * @since 2.10.0
 */
@Slf4j
@Component
public class IPUtils {

    private static final String DB_PATH = "ip2region.xdb";
    private static Searcher searcher;

    /**
     * 获取IP地址
     *
     * @param request HttpServletRequest对象
     * @return 客户端IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = null;
        try {
            if (request == null) {
                return "";
            }
            ip = request.getHeader(HttpHeaderConstants.X_FORWARDED_FOR);
            if (checkIp(ip)) {
                ip = request.getHeader(HttpHeaderConstants.PROXY_CLIENT_IP);
            }
            if (checkIp(ip)) {
                ip = request.getHeader(HttpHeaderConstants.WL_PROXY_CLIENT_IP);
            }
            if (checkIp(ip)) {
                ip = request.getHeader(HttpHeaderConstants.HTTP_CLIENT_IP);
            }
            if (checkIp(ip)) {
                ip = request.getHeader(HttpHeaderConstants.HTTP_X_FORWARDED_FOR);
            }
            if (checkIp(ip)) {
                ip = request.getRemoteAddr();
                if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                    // 根据网卡取本机配置的IP
                    ip = getLocalAddr();
                }
            }
        } catch (Exception e) {
            log.error("IPUtils ERROR, {}", e.getMessage());
        }

        // 使用代理，则获取第一个IP地址
        if (StringUtils.isNotBlank(ip) && ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(","));
        }

        return ip;
    }

    private static boolean checkIp(String ip) {
        return StringUtils.isEmpty(ip) || HttpHeaderConstants.UNKNOWN.equalsIgnoreCase(ip);
    }

    /**
     * 获取本机的IP地址
     *
     * @return 本机IP地址
     */
    private static String getLocalAddr() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("InetAddress.getLocalHost()-error, {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据IP地址获取地理位置信息
     *
     * @param ip IP地址
     * @return 地理位置信息
     */
    public static String getRegion(String ip) {
        if (searcher == null) {
            log.warn("IP2Region Searcher未初始化，无法获取地理位置信息");
            return "未知";
        }

        try {
            return searcher.search(ip);
        } catch (Exception e) {
            log.error("IP地理位置查询失败: {}", e.getMessage());
            return "未知";
        }
    }

    @PostConstruct
    public void init() {
        try {
            // 从类路径加载资源文件，尝试多种方式
            InputStream inputStream = null;
            String loadPath = "";

            // 方式1：使用ClassLoader加载data目录下的文件
            inputStream = this.getClass().getClassLoader().getResourceAsStream("data/ip2region.xdb");
            loadPath = "data/ip2region.xdb";

            if (inputStream == null) {
                // 方式2：使用相对路径加载
                inputStream = this.getClass().getResourceAsStream("/data/ip2region.xdb");
                loadPath = "/data/ip2region.xdb";
            }

            if (inputStream == null) {
                // 方式3：尝试直接从resources根目录加载
                inputStream = this.getClass().getClassLoader().getResourceAsStream(DB_PATH);
                loadPath = DB_PATH;
            }

            if (inputStream == null) {
                log.error("IP2Region数据库文件未找到，已尝试多种加载方式");
                return;
            }

            // 将资源文件复制到临时文件
            Path tempDbPath = Files.createTempFile("ip2region", ".xdb");
            Files.copy(inputStream, tempDbPath, StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();

            // 使用临时文件初始化 Searcher 对象
            searcher = Searcher.newWithFileOnly(tempDbPath.toString());
            log.info("IP2Region数据库初始化成功，加载路径: {}, 临时文件路径: {}", loadPath, tempDbPath.toString());
        } catch (Exception e) {
            log.error("IP2Region数据库初始化失败: {}", e.getMessage(), e);
        }
    }
}

