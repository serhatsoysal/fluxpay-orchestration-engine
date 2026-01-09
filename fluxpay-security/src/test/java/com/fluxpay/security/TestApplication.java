package com.fluxpay.security;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.fluxpay.security")
@EnableJpaRepositories(basePackages = "com.fluxpay.security.session.repository")
@EntityScan(basePackages = {"com.fluxpay.security.session.entity", "com.fluxpay.common.entity"})
@EnableJpaAuditing
public class TestApplication {
}

