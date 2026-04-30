package com.cybzacg.blogbackend.config;


import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import com.github.xiaoymin.knife4j.core.conf.ExtensionsConstants;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jSetting;
import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Knife4j OpenAPI 增强定制器。<p>在 OpenAPI 规范中注入 Knife4j 扩展属性（设置、Markdown 文档），并为 Controller 上的 {@code @ApiSupport} 注解生成 x-order 排序扩展。</p>
 */
@Primary
@Configuration
@Slf4j
@ConditionalOnBean(Knife4jProperties.class)
public class Knife4jOpenApiCustomizer implements GlobalOpenApiCustomizer {
    final Knife4jProperties knife4jProperties;
    final SpringDocConfigProperties properties;

    public Knife4jOpenApiCustomizer(Knife4jProperties knife4jProperties, SpringDocConfigProperties properties) {
        this.knife4jProperties = knife4jProperties;
        this.properties = properties;
    }

    @Override
    public void customise(OpenAPI openApi) {
        log.debug("Knife4j OpenApiCustomizer");
        
        // 添加 Knife4j 扩展属性
        if (knife4jProperties.isEnable()) {
            try {
                addKnife4jExtension(openApi);
            } catch (Exception e) {
                log.warn("Knife4j 扩展属性设置失败: {}", e.getMessage());
            }
        }
        
        // 添加 x-order 扩展（如果 Controller 有 @ApiSupport 注解）
        addOrderExtension(openApi);
    }

    /**
     * 添加 Knife4j 扩展属性
     */
    private void addKnife4jExtension(OpenAPI openApi) {
        Knife4jSetting setting = knife4jProperties.getSetting();
        if (setting == null) {
            log.warn("Knife4j setting is null, skip extension");
            return;
        }
        
        OpenApiExtensionResolver openApiExtensionResolver = new OpenApiExtensionResolver(setting, knife4jProperties.getDocuments());
        openApiExtensionResolver.start();
        
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("settings", setting);
        objectMap.put("markdownFiles", openApiExtensionResolver.getMarkdownFiles());
        openApi.addExtension("x-openapi-settings", objectMap);
    }

    /**
     * 往OpenAPI内tags字段添加x-order属性
     */
    private void addOrderExtension(OpenAPI openApi) {
        try {
            if (properties == null || CollectionUtils.isEmpty(properties.getGroupConfigs())) {
                return;
            }
            
            // 获取包扫描路径
            Set<String> packagesToScan = new HashSet<>();
            for (SpringDocConfigProperties.GroupConfig groupConfig : properties.getGroupConfigs()) {
                List<String> packages = groupConfig.getPackagesToScan();
                if (packages != null && !packages.isEmpty()) {
                    packagesToScan.addAll(packages);
                }
            }
            
            if (packagesToScan.isEmpty()) {
                return;
            }
            
            // 扫描包下被ApiSupport注解的RestController Class
            Set<Class<?>> classes = new HashSet<>();
            for (String packageToScan : packagesToScan) {
                Set<Class<?>> scanned = scanPackageByAnnotation(packageToScan, RestController.class);
                classes.addAll(scanned);
            }
            
            // 过滤有 @ApiSupport 注解的类
            Set<Class<?>> apiSupportClasses = new HashSet<>();
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(ApiSupport.class)) {
                    apiSupportClasses.add(clazz);
                }
            }
            
            if (!apiSupportClasses.isEmpty()) {
                // ApiSupport order值存入tagSortMap<Tag.name,ApiSupport.order>
                Map<String, Integer> tagOrderMap = new HashMap<>();
                for (Class<?> clazz : apiSupportClasses) {
                    Tag tag = getTag(clazz);
                    if (tag != null) {
                        ApiSupport apiSupport = clazz.getAnnotation(ApiSupport.class);
                        tagOrderMap.putIfAbsent(tag.name(), apiSupport.order());
                    }
                }
                
                // 往openApi tags字段添加x-order增强属性
                if (openApi.getTags() != null) {
                    for (io.swagger.v3.oas.models.tags.Tag tag : openApi.getTags()) {
                        if (tagOrderMap.containsKey(tag.getName())) {
                            tag.addExtension(ExtensionsConstants.EXTENSION_ORDER, tagOrderMap.get(tag.getName()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Knife4j 添加 x-order 扩展失败: {}", e.getMessage());
        }
    }

    private Tag getTag(Class<?> clazz) {
        // 从类上获取
        Tag tag = clazz.getAnnotation(Tag.class);
        if (tag == null) {
            // 从接口上获取
            Class<?>[] interfaces = clazz.getInterfaces();
            if (ArrayUtils.isNotEmpty(interfaces)) {
                for (Class<?> interfaceClazz : interfaces) {
                    Tag anno = interfaceClazz.getAnnotation(Tag.class);
                    if (anno != null) {
                        tag = anno;
                        break;
                    }
                }
            }
        }
        return tag;
    }

    private Set<Class<?>> scanPackageByAnnotation(String packageName, final Class<? extends Annotation> annotationClass) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(annotationClass));
            for (BeanDefinition beanDefinition : scanner.findCandidateComponents(packageName)) {
                Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
                classes.add(clazz);
            }
        } catch (ClassNotFoundException e) {
            log.warn("扫描包 {} 时发生 ClassNotFoundException: {}", packageName, e.getMessage());
        } catch (Exception e) {
            log.warn("扫描包 {} 时发生异常: {}", packageName, e.getMessage());
        }
        return classes;
    }
}