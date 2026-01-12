package com.fluxpay.notification.service;

import com.fluxpay.notification.entity.EmailTemplate;
import com.fluxpay.notification.repository.EmailTemplateRepository;
import com.fluxpay.security.context.TenantContext;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateRepository templateRepository;

    public EmailService(JavaMailSender mailSender, EmailTemplateRepository templateRepository) {
        this.mailSender = mailSender;
        this.templateRepository = templateRepository;
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendTemplatedEmail(String to, String templateName, Map<String, String> variables) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        EmailTemplate template = templateRepository.findByTenantIdAndName(tenantId, templateName)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateName));

        String subject = replacePlaceholders(template.getSubject(), variables);
        String htmlBody = replacePlaceholders(template.getHtmlBody(), variables);

        sendEmail(to, subject, htmlBody);
    }

    private String replacePlaceholders(String text, Map<String, String> variables) {
        String result = text;
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            result = result.replace("{{" + key + "}}", value);
        }
        
        return result;
    }
}

