package com.cybzacg.blogbackend.config;

import com.cybzacg.blogbackend.utils.StrUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Web 日期时间格式配置。
 * <p>支持多种日期时间格式的解析：
 * <ul>
 *   <li>yyyy-MM-dd HH:mm:ss（完整日期时间）</li>
 *   <li>yyyy-MM-dd（日期部分，自动补零）</li>
 * </ul>
 */
@Configuration
public class WebDateTimeFormatConfig implements WebMvcConfigurer {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToLocalDateTimeConverter());
        registry.addConverter(new StringToLocalDateConverter());
    }

    /**
     * String -> LocalDateTime 转换器，支持多种格式
     */
    private static class StringToLocalDateTimeConverter implements org.springframework.core.convert.converter.Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            if (!StrUtils.hasText(source)) {
                return null;
            }
            String trimmed = StrUtils.trim(source);
            if (trimmed.contains(":")) {
                return LocalDateTime.parse(trimmed, DATETIME_FORMATTER);
            } else {
                // 纯日期格式，自动补零
                LocalDate date = LocalDate.parse(trimmed, DATE_FORMATTER);
                return date.atStartOfDay();
            }
        }
    }

    /**
     * String -> LocalDate 转换器
     */
    private static class StringToLocalDateConverter implements org.springframework.core.convert.converter.Converter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            if (!StrUtils.hasText(source)) {
                return null;
            }
            return LocalDate.parse(StrUtils.trim(source), DATE_FORMATTER);
        }
    }
}