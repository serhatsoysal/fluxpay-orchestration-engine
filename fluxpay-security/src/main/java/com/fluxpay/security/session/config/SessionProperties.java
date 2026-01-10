package com.fluxpay.security.session.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "session")
public class SessionProperties {
    private Ttl ttl = new Ttl();
    private Concurrent concurrent = new Concurrent();
    private Security security = new Security();
    private Audit audit = new Audit();

    @Data
    public static class Ttl {
        private Duration accessToken = Duration.ofHours(1);
        private Duration refreshToken = Duration.ofDays(30);
    }

    @Data
    public static class Concurrent {
        private int maxSessions = 5;
    }

    @Data
    public static class Security {
        private boolean fingerprintVerification = true;
        private boolean anomalyDetection = true;
    }

    @Data
    public static class Audit {
        private int retentionDays = 365;
    }
}

