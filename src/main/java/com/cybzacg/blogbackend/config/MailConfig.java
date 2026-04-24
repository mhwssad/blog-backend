package com.cybzacg.blogbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件发送器配置。<p>手动创建 JavaMailSender Bean，以避免在使用非默认配置文件时 IDE 报找不到 Bean 的警告。通过读取 MailProperties 中的属性完成初始化。</p>
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    private final MailProperties mailProperties;

    public MailConfig(MailProperties mailProperties) {
        this.mailProperties = mailProperties;
    }

    /**
     * 创建并配置 JavaMailSender 实例。
     *
     * @return 配置好的 JavaMailSender 实例
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailProperties.getHost());
        mailSender.setPort(mailProperties.getPort());
        mailSender.setUsername(mailProperties.getUsername());
        mailSender.setPassword(mailProperties.getPassword());
        mailSender.setProtocol(mailProperties.getProtocol());
        if (mailProperties.getDefaultEncoding() != null) {
            mailSender.setDefaultEncoding(mailProperties.getDefaultEncoding().name());
        }

        Properties properties = mailSender.getJavaMailProperties();
        properties.putAll(mailProperties.getProperties());

        return mailSender;
    }
}
