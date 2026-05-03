package com.cybzacg.blogbackend.common.email;

/**
 * 统一邮件发送服务。
 *
 * <p>封装 JavaMailSender 的发信逻辑与异常处理，供全项目复用。
 */
public interface EmailService {

    /**
     * 发送纯文本邮件。
     *
     * @param to      收件人地址
     * @param subject 邮件主题
     * @param text    邮件正文
     */
    void sendTextEmail(String to, String subject, String text);
}
