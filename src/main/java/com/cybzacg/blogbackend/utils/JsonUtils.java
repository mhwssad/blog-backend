package com.cybzacg.blogbackend.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON序列化工具类
 * <p>
 * 基于Jackson {@link ObjectMapper} 实现，内部持有的 ObjectMapper 实例在类加载时完成配置，
 * 且 {@code ObjectMapper} 本身是线程安全的，因此本工具类的所有静态方法可安全地在多线程环境中并发调用。
 */
public class JsonUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    /**
     * 线程安全的ObjectMapper实例
     */
    private static final ObjectMapper OBJECT_MAPPER;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 初始化 ObjectMapper：注册 JavaTimeModule（配置 LocalDate/LocalDateTime 的序列化与反序列化格式），
    // 关闭将日期写为时间戳、空 Bean 序列化失败、未知属性反序列化失败等特性，并将空字符串视为 null。
    static {
        OBJECT_MAPPER = new ObjectMapper();

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(
            LocalDateTime.class,
            new LocalDateTimeSerializer(DATE_TIME_FORMATTER)
        );
        javaTimeModule.addDeserializer(
            LocalDateTime.class,
            new LocalDateTimeDeserializer(DATE_TIME_FORMATTER)
        );
        javaTimeModule.addSerializer(
            LocalDate.class,
            new LocalDateSerializer(DATE_FORMATTER)
        );
        javaTimeModule.addDeserializer(
            LocalDate.class,
            new LocalDateDeserializer(DATE_FORMATTER)
        );
        OBJECT_MAPPER.registerModule(javaTimeModule);

        OBJECT_MAPPER.configure(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
            false
        );
        OBJECT_MAPPER.configure(
            SerializationFeature.FAIL_ON_EMPTY_BEANS,
            false
        );
        OBJECT_MAPPER.configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );
        OBJECT_MAPPER.configure(
            DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
            true
        );
    }

    /**
     * 获取ObjectMapper实例
     *
     * @return 全局共享的、线程安全的 {@link ObjectMapper} 实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 对象转JSON字符串
     *
     * @param obj 待序列化的对象，可以为 {@code null}
     * @return JSON 字符串；若 {@code obj} 为 {@code null} 则返回 {@code null}
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON字符串失败", e);
            throw new RuntimeException("对象转JSON字符串失败", e);
        }
    }

    /**
     * 对象转格式化的JSON字符串
     *
     * @param obj 待序列化的对象，可以为 {@code null}
     * @return 格式化的（带缩进换行的）JSON 字符串；若 {@code obj} 为 {@code null} 则返回 {@code null}
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(
                obj
            );
        } catch (JsonProcessingException e) {
            log.error("对象转格式化JSON字符串失败", e);
            throw new RuntimeException("对象转格式化JSON字符串失败", e);
        }
    }

    /**
     * JSON字符串转对象
     *
     * @param json  JSON 字符串，可以为 {@code null} 或空串
     * @param clazz 目标类型的 {@link Class}
     * @param <T>   目标类型
     * @return 反序列化后的对象；若 {@code json} 为 {@code null} 或空白则返回 {@code null}
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON字符串转对象失败", e);
            throw new RuntimeException("JSON字符串转对象失败", e);
        }
    }

    /**
     * JSON字符串转对象（支持泛型）
     *
     * @param json          JSON 字符串，可以为 {@code null} 或空串
     * @param typeReference 目标类型的 {@link TypeReference}，用于保留泛型信息
     * @param <T>           目标类型
     * @return 反序列化后的对象；若 {@code json} 为 {@code null} 或空白则返回 {@code null}
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON字符串转对象失败", e);
            throw new RuntimeException("JSON字符串转对象失败", e);
        }
    }

    /**
     * JSON字符串转List
     *
     * @param json  JSON 字符串，可以为 {@code null} 或空串
     * @param clazz List 元素类型的 {@link Class}
     * @param <T>   List 元素类型
     * @return 反序列化后的 {@link List}；若 {@code json} 为 {@code null} 或空白则返回 {@code null}
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(
                json,
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(
                    List.class,
                    clazz
                )
            );
        } catch (JsonProcessingException e) {
            log.error("JSON字符串转List失败", e);
            throw new RuntimeException("JSON字符串转List失败", e);
        }
    }

    /**
     * JSON字符串转Map
     *
     * @param json JSON 字符串，可以为 {@code null} 或空串
     * @return 反序列化后的 {@code Map<String, Object>}；若 {@code json} 为 {@code null} 或空白则返回 {@code null}
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON字符串转Map失败", e);
            throw new RuntimeException("JSON字符串转Map失败", e);
        }
    }

    /**
     * 对象转Map
     *
     * @param obj 待转换的对象，可以为 {@code null}
     * @return 转换后的 {@code Map<String, Object>}；若 {@code obj} 为 {@code null} 则返回 {@code null}
     * @throws RuntimeException 转换失败时抛出
     */
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.convertValue(obj, new TypeReference<>() {});
        } catch (IllegalArgumentException e) {
            log.error("对象转Map失败", e);
            throw new RuntimeException("对象转Map失败", e);
        }
    }

    /**
     * Map转对象
     *
     * @param map   待转换的 Map，可以为 {@code null}
     * @param clazz 目标类型的 {@link Class}
     * @param <T>   目标类型
     * @return 转换后的对象；若 {@code map} 为 {@code null} 则返回 {@code null}
     * @throws RuntimeException 转换失败时抛出
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.convertValue(map, clazz);
        } catch (IllegalArgumentException e) {
            log.error("Map转对象失败", e);
            throw new RuntimeException("Map转对象失败", e);
        }
    }

    /**
     * 解析JSON字符串为JsonNode
     *
     * @param json JSON 字符串，可以为 {@code null} 或空串
     * @return 解析后的 {@link JsonNode}；若 {@code json} 为 {@code null} 或空白则返回 {@code null}
     * @throws RuntimeException 解析失败时抛出
     */
    public static JsonNode parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("解析JSON字符串失败", e);
            throw new RuntimeException("解析JSON字符串失败", e);
        }
    }

    /**
     * 深度复制对象
     * <p>
     * 通过先将对象序列化为 JSON 再反序列化回新对象来实现深拷贝。
     *
     * @param obj   待复制的对象，可以为 {@code null}
     * @param clazz 目标类型的 {@link Class}
     * @param <T>   目标类型
     * @return 深拷贝后的新对象；若 {@code obj} 为 {@code null} 则返回 {@code null}
     * @throws RuntimeException 序列化或反序列化失败时抛出
     */
    public static <T> T deepCopy(T obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }

        try {
            String json = OBJECT_MAPPER.writeValueAsString(obj);
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("深度复制对象失败", e);
            throw new RuntimeException("深度复制对象失败", e);
        }
    }

    /**
     * 检查字符串是否为有效的JSON
     *
     * @param json 待检查的字符串，可以为 {@code null} 或空串
     * @return 若字符串可被成功解析为 JSON 则返回 {@code true}；否则返回 {@code false}；
     *         若字符串为 {@code null} 或空白也返回 {@code false}
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
