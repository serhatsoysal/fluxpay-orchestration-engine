package com.fluxpay.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.fluxpay")
@EnableJpaRepositories(basePackages = "com.fluxpay")
@EntityScan(basePackages = "com.fluxpay")
@EnableJpaAuditing
@EnableScheduling
public class FluxPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(FluxPayApplication.class, args);
    }
}

