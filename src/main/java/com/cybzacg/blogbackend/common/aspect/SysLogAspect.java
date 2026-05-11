package com.cybzacg.blogbackend.common.aspect;

import com.cybzacg.blogbackend.common.annotation.DisableSysLog;
import com.cybzacg.blogbackend.dto.domain.system.SysLog;
import com.cybzacg.blogbackend.dto.repository.auth.audit.SysLogRepository;
import com.cybzacg.blogbackend.utils.IPUtils;
import com.cybzacg.blogbackend.utils.JsonUtils;
import com.cybzacg.blogbackend.utils.SecurityUtils;
import com.cybzacg.blogbackend.utils.StrUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 控制器操作日志切面。
 *
 * <p>职责：
 * <p>1. 拦截后台和用户侧的操作型接口。
 * <p>2. 采集请求、响应、用户、IP、UA 和耗时信息。
 * <p>3. 对敏感字段进行脱敏后写入 sys_log 表。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class SysLogAspect {
    private static final Logger log = LoggerFactory.getLogger(SysLogAspect.class);
    /**
     * 请求参数日志最大长度，避免超长文本撑爆数据库字段。
     */
    private static final int MAX_REQUEST_LENGTH = 8000;

    /**
     * 响应日志最大长度，避免大量分页数据或异常堆栈写入过长。
     */
    private static final int MAX_RESPONSE_LENGTH = 12000;

    /**
     * 需要统一脱敏的字段名，比较时会忽略下划线和大小写。
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "oldpassword", "newpassword", "confirmpassword",
            "token", "accesstoken", "refreshtoken", "authorization",
            "secret", "secretkey", "credential", "credentials",
            "captcha", "emailcode", "code"
    );

    private final SysLogRepository sysLogRepository;

    /**
     * 拦截所有 RestController 的公开方法，再在运行时按 URI 和注解进一步筛选。
     * 包装控制器调用，确保业务成功或异常退出时都能补记系统操作日志。
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * com.cybzacg.blogbackend..controller..*(..))")
    public Object recordSysLog(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        Method method = resolveMethod(joinPoint);
        Class<?> targetClass = AopUtils.getTargetClass(joinPoint.getTarget());

        // 无 Web 请求上下文，或当前接口被排除时，直接执行原逻辑。
        if (request == null || shouldSkip(request, targetClass, method)) {
            return joinPoint.proceed();
        }

        Instant start = Instant.now();
        Object result = null;
        Throwable throwable = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            // 业务异常也要记录到日志表，因此先缓存异常，再继续抛出。
            throwable = ex;
            throw ex;
        } finally {
            try {
                persistLog(joinPoint, targetClass, method, request, result, throwable,
                        Duration.between(start, Instant.now()).toMillis());
            } catch (Exception logEx) {
                // 日志记录失败不能反向影响主业务请求。
                log.error("记录系统操作日志失败", logEx);
            }
        }
    }

    /**
     * 组装并保存一条系统操作日志。
     */
    private void persistLog(ProceedingJoinPoint joinPoint,
                            Class<?> targetClass,
                            Method method,
                            HttpServletRequest request,
                            Object result,
                            Throwable throwable,
                            long executionTime) {
        SysLog sysLog = new SysLog();
        sysLog.setModule(resolveModule(targetClass));
        sysLog.setContent(resolveContent(method, request));
        sysLog.setRequestMethod(request.getMethod());
        sysLog.setRequestParams(serializeArguments(joinPoint.getArgs()));
        sysLog.setResponseContent(serializeResponse(result, throwable));
        sysLog.setRequestUri(request.getRequestURI());
        sysLog.setMethod(targetClass.getName() + "#" + method.getName());
        sysLog.setExecutionTime(executionTime);
        sysLog.setCreateBy(SecurityUtils.getUserId());
        sysLog.setCreateTime(LocalDateTime.now());

        String ip = IPUtils.getIpAddr(request);
        sysLog.setIp(ip);
        fillRegion(sysLog, ip);
        fillUserAgent(sysLog, request.getHeader("User-Agent"));

        sysLogRepository.saveLog(sysLog);
    }

    /**
     * 判断当前请求是否应跳过日志记录。
     *
     * <p>这里同时支持：
     * <p>1. 只记录指定 URI 前缀。
     * <p>2. 通过 @DisableSysLog 在类或方法维度显式禁用。
     */
    private boolean shouldSkip(HttpServletRequest request, Class<?> targetClass, Method method) {
        if (!shouldLogUri(request.getRequestURI())) {
            return true;
        }
        return AnnotatedElementUtils.hasAnnotation(method, DisableSysLog.class)
                || AnnotatedElementUtils.hasAnnotation(targetClass, DisableSysLog.class);
    }

    /**
     * 目前仅记录后台接口和用户行为接口，并排除日志管理自身，避免递归写日志。
     */
    private boolean shouldLogUri(String uri) {
        if (!StrUtils.hasText(uri)) {
            return false;
        }
        if (uri.startsWith("/api/sys/logs")) {
            return false;
        }
        return uri.startsWith("/api/sys/") || uri.startsWith("/api/user/");
    }

    /**
     * 获取代理后的真实方法，避免只拿到接口方法导致注解读取不完整。
     */
    private Method resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());
    }

    /**
     * 从当前线程上下文中获取请求对象。
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    /**
     * 优先使用 Swagger 的 Tag 名称作为日志模块，便于和接口文档保持一致。
     */
    private String resolveModule(Class<?> targetClass) {
        Tag tag = AnnotatedElementUtils.findMergedAnnotation(targetClass, Tag.class);
        if (tag != null && StrUtils.hasText(tag.name())) {
            return tag.name();
        }
        return targetClass.getSimpleName();
    }

    /**
     * 优先使用 Operation.summary 作为日志内容，没有则回退到“请求方法 + 路径”。
     */
    private String resolveContent(Method method, HttpServletRequest request) {
        Operation operation = AnnotatedElementUtils.findMergedAnnotation(method, Operation.class);
        if (operation != null && StrUtils.hasText(operation.summary())) {
            return operation.summary();
        }
        return request.getMethod() + " " + request.getRequestURI();
    }

    /**
     * 将 IP 解析出的地区信息拆成省份和城市落库。
     */
    private void fillRegion(SysLog sysLog, String ip) {
        if (!StrUtils.hasText(ip)) {
            return;
        }
        String region = IPUtils.getRegion(ip);
        if (!StrUtils.hasText(region) || "未知".equals(region)) {
            return;
        }
        String[] parts = region.split("\\|");
        if (parts.length >= 4) {
            sysLog.setProvince(normalizeRegionPart(parts[2]));
            sysLog.setCity(normalizeRegionPart(parts[3]));
        }
    }

    /**
     * ip2region 对未知值会返回 0 / 未知 / 内网IP，这里统一转成 null。
     */
    private String normalizeRegionPart(String value) {
        if (!StrUtils.hasText(value) || "0".equals(value) || "内网IP".equals(value) || "未知".equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * 解析 User-Agent，尽量提取出浏览器、版本和操作系统，失败时允许为空或 Unknown。
     */
    private void fillUserAgent(SysLog sysLog, String userAgent) {
        if (!StrUtils.hasText(userAgent)) {
            return;
        }
        String lower = userAgent.toLowerCase(Locale.ROOT);
        sysLog.setBrowser(resolveBrowser(lower));
        sysLog.setBrowserVersion(resolveBrowserVersion(userAgent, lower));
        sysLog.setOs(resolveOs(lower));
    }

    /**
     * 根据 User-Agent 关键字推断浏览器类型。
     */
    private String resolveBrowser(String lowerUserAgent) {
        if (lowerUserAgent.contains("edg/")) {
            return "Edge";
        }
        if (lowerUserAgent.contains("chrome/")) {
            return "Chrome";
        }
        if (lowerUserAgent.contains("firefox/")) {
            return "Firefox";
        }
        if (lowerUserAgent.contains("safari/") && lowerUserAgent.contains("version/")) {
            return "Safari";
        }
        if (lowerUserAgent.contains("postmanruntime/")) {
            return "Postman";
        }
        if (lowerUserAgent.contains("okhttp/")) {
            return "OkHttp";
        }
        return "Unknown";
    }

    /**
     * 按不同浏览器标识提取版本号，避免统一规则误判。
     */
    private String resolveBrowserVersion(String userAgent, String lowerUserAgent) {
        if (lowerUserAgent.contains("edg/")) {
            return extractVersion(userAgent, "Edg/");
        }
        if (lowerUserAgent.contains("chrome/")) {
            return extractVersion(userAgent, "Chrome/");
        }
        if (lowerUserAgent.contains("firefox/")) {
            return extractVersion(userAgent, "Firefox/");
        }
        if (lowerUserAgent.contains("safari/") && lowerUserAgent.contains("version/")) {
            return extractVersion(userAgent, "Version/");
        }
        if (lowerUserAgent.contains("postmanruntime/")) {
            return extractVersion(userAgent, "PostmanRuntime/");
        }
        if (lowerUserAgent.contains("okhttp/")) {
            return extractVersion(userAgent, "okhttp/");
        }
        return null;
    }

    /**
     * 从 User-Agent 中截取版本号片段。
     */
    private String extractVersion(String userAgent, String marker) {
        int start = userAgent.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = start;
        while (end < userAgent.length()) {
            char c = userAgent.charAt(end);
            if (Character.isDigit(c) || c == '.') {
                end++;
                continue;
            }
            break;
        }
        return start == end ? null : userAgent.substring(start, end);
    }

    /**
     * 根据 User-Agent 关键字归类操作系统名称。
     */
    private String resolveOs(String lowerUserAgent) {
        if (lowerUserAgent.contains("windows")) {
            return "Windows";
        }
        if (lowerUserAgent.contains("mac os x") || lowerUserAgent.contains("macintosh")) {
            return "macOS";
        }
        if (lowerUserAgent.contains("android")) {
            return "Android";
        }
        if (lowerUserAgent.contains("iphone") || lowerUserAgent.contains("ipad") || lowerUserAgent.contains("ios")) {
            return "iOS";
        }
        if (lowerUserAgent.contains("linux")) {
            return "Linux";
        }
        return "Unknown";
    }

    /**
     * 序列化方法参数，并过滤 Servlet / 文件上传等不适合写入日志的对象。
     */
    private String serializeArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> values = new ArrayList<>();
        for (Object arg : args) {
            if (shouldSerialize(arg)) {
                values.add(sanitizeValue(arg));
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        return truncate(toJsonSafely(values), MAX_REQUEST_LENGTH);
    }

    /**
     * 序列化响应体；如果接口抛异常，则写入异常类型和消息，便于排查。
     */
    private String serializeResponse(Object result, Throwable throwable) {
        if (throwable != null) {
            String message = throwable.getClass().getSimpleName();
            if (StrUtils.hasText(throwable.getMessage())) {
                message += ": " + throwable.getMessage();
            }
            return truncate(message, MAX_RESPONSE_LENGTH);
        }
        if (result == null) {
            return null;
        }
        return truncate(toJsonSafely(sanitizeValue(result)), MAX_RESPONSE_LENGTH);
    }

    /**
     * 先转成 JsonNode，再递归脱敏，避免直接修改原始对象。
     */
    private Object sanitizeValue(Object value) {
        try {
            var node = JsonUtils.getObjectMapper().valueToTree(value);
            redactSensitiveFields(node);
            return node;
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    /**
     * 递归遍历对象树，对敏感字段值统一替换为 ***。
     */
    private void redactSensitiveFields(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (isSensitiveField(fieldName)) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(fieldName, "***");
                } else {
                    redactSensitiveFields(node.get(fieldName));
                }
            }
            return;
        }
        if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode child : node) {
                redactSensitiveFields(child);
            }
        }
    }

    private boolean isSensitiveField(String fieldName) {
        if (!StrUtils.hasText(fieldName)) {
            return false;
        }
        return SENSITIVE_FIELDS.contains(fieldName.replace("_", "").toLowerCase(Locale.ROOT));
    }

    /**
     * JSON 序列化失败时回退为字符串，保证日志记录流程不中断。
     */
    private String toJsonSafely(Object value) {
        try {
            return JsonUtils.toJson(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    /**
     * 日志字段统一截断，防止请求体或响应体过大。
     */
    private String truncate(String value, int maxLength) {
        if (!StrUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * 过滤不适合直接序列化到日志里的参数类型。
     */
    private boolean shouldSerialize(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof ServletRequest
                || value instanceof ServletResponse
                || value instanceof BindingResult
                || value instanceof Principal) {
            return false;
        }
        if (value instanceof MultipartFile || value instanceof MultipartFile[]) {
            return false;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof MultipartFile) {
                    return false;
                }
            }
        }
        if (value.getClass().isArray()) {
            return !MultipartFile.class.isAssignableFrom(value.getClass().getComponentType());
        }
        return true;
    }
}




