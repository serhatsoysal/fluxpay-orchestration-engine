package com.fluxpay.security.session.service;

import com.fluxpay.security.session.model.DeviceInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import ua_parser.Client;
import ua_parser.Parser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class DeviceFingerprintService {

    private final Parser uaParser;

    public DeviceFingerprintService() {
        this.uaParser = new Parser();
    }

    public String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");
        
        String rawFingerprint = String.join("|", 
                userAgent != null ? userAgent : "",
                acceptLanguage != null ? acceptLanguage : "",
                acceptEncoding != null ? acceptEncoding : ""
        );
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawFingerprint.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return rawFingerprint;
        }
    }

    public DeviceInfo extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        
        if (userAgent == null) {
            return DeviceInfo.builder().build();
        }
        
        Client client = uaParser.parse(userAgent);
        
        return DeviceInfo.builder()
                .deviceId(generateFingerprint(request))
                .deviceType(determineDeviceType(userAgent))
                .os(client.os.family)
                .osVersion(client.os.major != null ? client.os.major : "")
                .browser(client.userAgent.family)
                .browserVersion(client.userAgent.major != null ? client.userAgent.major : "")
                .build();
    }

    private String determineDeviceType(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "tablet";
        } else {
            return "desktop";
        }
    }

    public String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                return ip.trim();
            }
        }
        
        return request.getRemoteAddr();
    }
}

