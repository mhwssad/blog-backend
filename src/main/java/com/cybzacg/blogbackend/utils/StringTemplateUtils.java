package com.cybzacg.blogbackend.utils;

import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串模板工具类
 * <p>
 * 提供字符串模板替换功能，支持多种占位符格式和参数类型
 * </p>
 *
 * <p>支持的占位符格式：</p>
 * <ul>
 *   <li>${key} - Spring风格占位符</li>
 *   <li>{key} - 简化风格占位符</li>
 *   <li>#{key} - SpEL风格占位符</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 使用Map参数
 * Map<String, Object> params = new HashMap<>();
 * params.put("name", "张三");
 * params.put("age", 25);
 * String result = StringTemplateUtils.render("姓名：${name}，年龄：${age}", params);
 *
 * // 使用对象参数
 * User user = new User("李四", 30);
 * String result = StringTemplateUtils.render("用户：#{user.name}，年龄：#{user.age}", user);
 *
 * // 使用默认值
 * String result = StringTemplateUtils.render("欢迎，${name:访客}！", null);
 * }</pre>
 *
 * @author cybzacg
 * @since 1.0.0
 */
public final class StringTemplateUtils {

    /**
     * Spring风格占位符模式：${key}
     */
    private static final Pattern SPRING_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?\\}");

    /**
     * 简化风格占位符模式：{key}
     */
    private static final Pattern SIMPLE_PATTERN = Pattern.compile("\\{([^}:]+)(?::([^}]*))?\\}");

    /**
     * SpEL风格占位符模式：#{key}
     */
    private static final Pattern SPEL_PATTERN = Pattern.compile("#\\{([^}:]+)(?::([^}]*))?\\}");
    /**
     * 索引占位符模式：{数字} 或 {}
     * 匹配 {0}、{1}、{2} 等以及 {}
     */
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\{(\\d*)\\}");

    /**
     * 私有构造函数，防止实例化
     */
    private StringTemplateUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 渲染字符串模板（自动识别占位符格式）
     * <p>
     * 支持的占位符格式：${key}、{key}、#{key}
     * </p>
     *
     * @param template 模板字符串
     * @param params   参数Map
     * @return 渲染后的字符串
     */
    public static String render(String template, Map<String, Object> params) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        if (params == null || params.isEmpty()) {
            return template;
        }
        return renderWithPattern(template, params, SPRING_PATTERN);
    }

    /**
     * 渲染字符串模板（使用对象属性）
     * <p>
     * 支持通过对象属性访问，如：#{user.name}、#{user.age}
     * </p>
     *
     * @param template 模板字符串
     * @param object   参数对象
     * @return 渲染后的字符串
     */
    public static String render(String template, Object object) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        if (object == null) {
            return template;
        }
        return renderWithObject(template, object, SPEL_PATTERN);
    }

    /**
     * 渲染字符串模板（简化风格占位符）
     * <p>
     * 使用 {key} 格式的占位符
     * </p>
     *
     * @param template 模板字符串
     * @param params   参数Map
     * @return 渲染后的字符串
     */
    public static String renderSimple(String template, Map<String, Object> params) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        if (params == null || params.isEmpty()) {
            return template;
        }
        return renderWithPattern(template, params, SIMPLE_PATTERN);
    }

    /**
     * 渲染字符串模板（Spring风格占位符）
     * <p>
     * 使用 ${key} 格式的占位符
     * </p>
     *
     * @param template 模板字符串
     * @param params   参数Map
     * @return 渲染后的字符串
     */
    public static String renderSpring(String template, Map<String, Object> params) {
        return render(template, params);
    }

    /**
     * 渲染字符串模板（SpEL风格占位符）
     * <p>
     * 使用 #{key} 格式的占位符
     * </p>
     *
     * @param template 模板字符串
     * @param object   参数对象
     * @return 渲染后的字符串
     */
    public static String renderSpEL(String template, Object object) {
        return render(template, object);
    }

    /**
     * 使用指定模式渲染模板
     *
     * @param template 模板字符串
     * @param params   参数Map
     * @param pattern  占位符模式
     * @return 渲染后的字符串
     */
    private static String renderWithPattern(String template, Map<String, Object> params, Pattern pattern) {
        Matcher matcher = pattern.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String defaultValue = matcher.group(2);
            Object value = params.get(key);

            String replacement;
            if (value != null) {
                replacement = Matcher.quoteReplacement(value.toString());
            } else if (defaultValue != null) {
                replacement = Matcher.quoteReplacement(defaultValue);
            } else {
                replacement = "";
            }

            matcher.appendReplacement(result, replacement);
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 使用对象属性渲染模板
     *
     * @param template 模板字符串
     * @param object   参数对象
     * @param pattern  占位符模式
     * @return 渲染后的字符串
     */
    private static String renderWithObject(String template, Object object, Pattern pattern) {
        Matcher matcher = pattern.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String defaultValue = matcher.group(2);

            Object value = getPropertyValue(object, key);
            String replacement;

            if (value != null) {
                replacement = Matcher.quoteReplacement(value.toString());
            } else if (defaultValue != null) {
                replacement = Matcher.quoteReplacement(defaultValue);
            } else {
                replacement = "";
            }

            matcher.appendReplacement(result, replacement);
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 获取对象属性值
     * <p>
     * 支持嵌套属性访问，如：user.name
     * </p>
     *
     * @param object 对象
     * @param key    属性键（支持点号分隔的嵌套属性）
     * @return 属性值
     */
    private static Object getPropertyValue(Object object, String key) {
        if (object == null || !StringUtils.hasText(key)) {
            return null;
        }

        String[] keys = key.split("\\.");
        Object current = object;

        try {
            for (String k : keys) {
                if (current == null) {
                    return null;
                }

                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(k);
                } else {
                    current = ReflectionUtils.readProperty(current, k).orElse(null);
                }
            }
            return current;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 批量替换字符串中的占位符
     * <p>
     * 使用Map中的键值对批量替换字符串中的占位符
     * </p>
     *
     * @param template 模板字符串
     * @param params   参数Map
     * @return 替换后的字符串
     */
    public static String replaceAll(String template, Map<String, Object> params) {
        return render(template, params);
    }

    /**
     * 检查模板中是否包含占位符
     *
     * @param template 模板字符串
     * @return 是否包含占位符
     */
    public static boolean hasPlaceholders(String template) {
        if (!StringUtils.hasText(template)) {
            return false;
        }
        return SPRING_PATTERN.matcher(template).find() ||
                SIMPLE_PATTERN.matcher(template).find() ||
                SPEL_PATTERN.matcher(template).find();
    }

    /**
     * 提取模板中的所有占位符键
     *
     * @param template 模板字符串
     * @return 占位符键集合
     */
    public static java.util.Set<String> extractPlaceholders(String template) {
        java.util.Set<String> placeholders = new java.util.LinkedHashSet<>();

        if (!StringUtils.hasText(template)) {
            return placeholders;
        }

        extractPlaceholdersWithPattern(template, SPRING_PATTERN, placeholders);
        extractPlaceholdersWithPattern(template, SIMPLE_PATTERN, placeholders);
        extractPlaceholdersWithPattern(template, SPEL_PATTERN, placeholders);

        return placeholders;
    }

    /**
     * 使用指定模式提取占位符
     *
     * @param template     模板字符串
     * @param pattern      占位符模式
     * @param placeholders 占位符集合
     */
    private static void extractPlaceholdersWithPattern(String template, Pattern pattern, java.util.Set<String> placeholders) {
        Matcher matcher = pattern.matcher(template);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
    }

    /**
     * 格式化字符串（使用索引占位符）
     * <p>
     * 使用 {0}、{1}、{2} 等索引占位符
     * </p>
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        if (args == null || args.length == 0) {
            return template;
        }
        return String.format(template, args);
    }

    /**
     * 安全格式化字符串
     * <p>
     * 当参数不足时，保留原始占位符而不抛出异常
     * </p>
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 格式化后的字符串
     */
    public static String safeFormat(String template, Object... args) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        if (args == null || args.length == 0) {
            return template;
        }

        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }

    /**
     * 按索引占位符渲染字符串
     * <p>
     * 支持 {} 和 {0}、{1}、{2} 等索引占位符格式，参数按顺序替换
     * </p>
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * // 使用空占位符（按顺序替换）
     * String result1 = StringTemplateUtils.renderByIndex("姓名：{}，年龄：{}", "张三", 25);
     * // 结果：姓名：张三，年龄：25
     *
     * // 使用数字占位符（按索引替换）
     * String result2 = StringTemplateUtils.renderByIndex("用户：{0}，年龄：{1}，职位：{0}", "管理员", 30);
     * // 结果：用户：管理员，年龄：30，职位：管理员
     *
     * // 混合使用
     * String result3 = StringTemplateUtils.renderByIndex("今天是{}，天气{}，温度{1}度", "周一", "晴", 25);
     * // 结果：今天是周一，天气晴，温度25度
     * }</pre>
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 渲染后的字符串
     */
    public static String renderByIndex(String template, Object... args) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        if (args == null || args.length == 0) {
            return template;
        }

        Matcher matcher = INDEX_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        int sequentialIndex = 0; // 用于空占位符 {} 的顺序索引

        while (matcher.find()) {
            String indexStr = matcher.group(1);
            String replacement;

            if (StringUtils.hasText(indexStr)) {
                // 数字占位符 {0}、{1} 等
                try {
                    int index = Integer.parseInt(indexStr);
                    if (index >= 0 && index < args.length) {
                        replacement = Matcher.quoteReplacement(args[index] != null ? args[index].toString() : "");
                    } else {
                        // 索引越界，保留原占位符
                        replacement = Matcher.quoteReplacement(matcher.group(0));
                    }
                } catch (NumberFormatException e) {
                    // 无效的索引格式，保留原占位符
                    replacement = Matcher.quoteReplacement(matcher.group(0));
                }
            } else {
                // 空占位符 {}，按顺序使用参数
                if (sequentialIndex < args.length) {
                    replacement = Matcher.quoteReplacement(args[sequentialIndex] != null ? args[sequentialIndex].toString() : "");
                    sequentialIndex++;
                } else {
                    // 参数不足，保留原占位符
                    replacement = Matcher.quoteReplacement(matcher.group(0));
                }
            }

            matcher.appendReplacement(result, replacement);
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 按索引占位符渲染字符串（安全模式）
     * <p>
     * 当参数不足或索引越界时，保留原始占位符而不抛出异常
     * </p>
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 渲染后的字符串
     */
    public static String safeRenderByIndex(String template, Object... args) {
        return renderByIndex(template, args);
    }
}

