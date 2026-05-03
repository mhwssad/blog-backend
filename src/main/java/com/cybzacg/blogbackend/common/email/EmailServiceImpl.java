package com.cybzacg.blogbackend.common.email;

import com.cybzacg.blogbackend.enums.error.ResultErrorCode;
import com.cybzacg.blogbackend.utils.ExceptionThrowerCore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 统一邮件发送服务实现。
 */
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendTextEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveMailFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            javaMailSender.send(message);
        } catch (Exception ex) {
            ExceptionThrowerCore.throwBusinessEx(ResultErrorCode.EMAIL_CAPTCHA_SEND_FAILED, ex);
        }
    }

    private String resolveMailFrom() {
        String from = mailProperties.getUsername();
        ExceptionThrowerCore.throwBusinessIfBlank(from, ResultErrorCode.EMAIL_CAPTCHA_SEND_FAILED);
        return from;
    }
}
