package com.fluxpay.notification.service;

import com.fluxpay.notification.entity.EmailTemplate;
import com.fluxpay.notification.repository.EmailTemplateRepository;
import com.fluxpay.security.context.TenantContext;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailTemplateRepository templateRepository;

    @InjectMocks
    private EmailService emailService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void sendEmail_ShouldSendMimeMessage() {
        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendEmail("test@example.com", "Test Subject", "<p>Test content</p>");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendTemplatedEmail_WithValidTemplate_ShouldSendEmail() {
        EmailTemplate template = new EmailTemplate();
        template.setId(UUID.randomUUID());
        template.setName("welcome");
        template.setSubject("Welcome {{name}}!");
        template.setHtmlBody("<p>Hello {{name}}</p>");

        when(templateRepository.findByTenantIdAndName(any(), any())).thenReturn(Optional.of(template));

        MimeMessage message = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(message);

        Map<String, String> variables = new HashMap<>();
        variables.put("name", "John");

        emailService.sendTemplatedEmail("test@example.com", "welcome", variables);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendTemplatedEmail_WithMissingTemplate_ShouldThrowException() {
        when(templateRepository.findByTenantIdAndName(any(), any())).thenReturn(Optional.empty());

        Map<String, String> variables = new HashMap<>();
        
        assertThatThrownBy(() -> emailService.sendTemplatedEmail("test@example.com", "missing", variables))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Template not found");
    }
}

