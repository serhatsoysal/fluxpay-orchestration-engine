package com.fluxpay.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null) {
            databaseUrl = System.getenv("DATABASE_URL");
        }
        
        String dbHost = environment.getProperty("DB_HOST");
        if (dbHost == null) {
            dbHost = System.getenv("DB_HOST");
        }
        
        String urlToParse = null;
        if (StringUtils.hasText(databaseUrl)) {
            urlToParse = databaseUrl;
        } else if (StringUtils.hasText(dbHost) && (dbHost.contains("://") || dbHost.contains("@"))) {
            urlToParse = dbHost;
        }
        
        if (urlToParse == null) {
            return;
        }
        
        final String finalUrlToParse = urlToParse;
        if (finalUrlToParse.startsWith("jdbc:")) {
            return;
        }
        
        try {
            String url = finalUrlToParse.trim();
            String username = null;
            String password = null;
            String host = null;
            int port = 5432;
            String database = null;
            
            if (url.startsWith("postgresql://") || url.startsWith("postgres://")) {
                url = url.replaceFirst("^postgresql://", "").replaceFirst("^postgres://", "");
            }
            
            int atIndex = url.indexOf('@');
            if (atIndex > 0) {
                String userInfo = url.substring(0, atIndex);
                String[] userParts = userInfo.split(":", 2);
                username = userParts.length > 0 ? userParts[0] : null;
                password = userParts.length > 1 ? userParts[1] : null;
                
                String hostDbPart = url.substring(atIndex + 1);
                int slashIndex = hostDbPart.indexOf('/');
                if (slashIndex > 0) {
                    String hostPort = hostDbPart.substring(0, slashIndex);
                    database = hostDbPart.substring(slashIndex + 1);
                    
                    if (hostPort.contains(":")) {
                        String[] hostPortParts = hostPort.split(":", 2);
                        host = hostPortParts[0];
                        if (hostPortParts.length > 1) {
                            try {
                                port = Integer.parseInt(hostPortParts[1]);
                            } catch (NumberFormatException e) {
                                port = 5432;
                            }
                        }
                    } else {
                        host = hostPort;
                    }
                }
            }
            
            if (host != null && database != null) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("spring.datasource.url", String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
                if (username != null) {
                    properties.put("spring.datasource.username", username);
                }
                if (password != null) {
                    properties.put("spring.datasource.password", password);
                }
                
                MapPropertySource propertySource = new MapPropertySource("databaseUrlConfig", properties);
                environment.getPropertySources().addFirst(propertySource);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse database URL: " + finalUrlToParse, e);
        }
    }
}

