package com.fluxpay.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.fluxpay")
@EnableJpaRepositories(basePackages = "com.fluxpay")
@EntityScan(basePackages = "com.fluxpay")
@EnableJpaAuditing
public class FluxPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(FluxPayApplication.class, args);
    }
}

