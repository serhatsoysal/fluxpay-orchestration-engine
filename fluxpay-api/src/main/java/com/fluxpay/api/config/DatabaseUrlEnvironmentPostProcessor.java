package com.fluxpay.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DATABASE_URL_PROPERTY = "DATABASE_URL";
    private static final String SPRING_DATASOURCE_URL_PROPERTY = "SPRING_DATASOURCE_URL";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = getEnvironmentVariable(DATABASE_URL_PROPERTY);
        if (databaseUrl == null) {
            databaseUrl = getEnvironmentVariable(SPRING_DATASOURCE_URL_PROPERTY);
        }
        
        if (databaseUrl == null) {
            String dbHost = getEnvironmentVariable("DB_HOST");
            if (dbHost != null && (dbHost.contains("://") || dbHost.contains("@"))) {
                databaseUrl = dbHost;
            }
        }
        
        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }
        
        String urlToParse = databaseUrl.trim();
        if (urlToParse.startsWith("jdbc:")) {
            return;
        }
        
        try {
            ParsedDatabaseUrl parsed = parseDatabaseUrl(urlToParse);
            if (parsed != null && parsed.isValid()) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("spring.datasource.url", parsed.getJdbcUrl());
                if (parsed.getUsername() != null) {
                    properties.put("spring.datasource.username", parsed.getUsername());
                }
                if (parsed.getPassword() != null) {
                    properties.put("spring.datasource.password", parsed.getPassword());
                }
                
                MapPropertySource propertySource = new MapPropertySource("databaseUrlProcessor", properties);
                MutablePropertySources propertySources = environment.getPropertySources();
                
                String applicationConfigSourceName = "applicationConfig: [classpath:/application.yml]";
                if (propertySources.contains(applicationConfigSourceName)) {
                    propertySources.addBefore(applicationConfigSourceName, propertySource);
                } else if (propertySources.contains("systemProperties")) {
                    propertySources.addBefore("systemProperties", propertySource);
                } else {
                    propertySources.addFirst(propertySource);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse database URL: " + databaseUrl, e);
        }
    }
    
    private String getEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null) {
            value = System.getProperty(name);
        }
        return value;
    }
    
    private ParsedDatabaseUrl parseDatabaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        url = url.trim();
        
        if (url.startsWith("postgresql://")) {
            url = url.substring(13);
        } else if (url.startsWith("postgres://")) {
            url = url.substring(11);
        }
        
        int atIndex = url.indexOf('@');
        if (atIndex <= 0) {
            return null;
        }
        
        String userInfo = url.substring(0, atIndex);
        String[] userParts = userInfo.split(":", 2);
        String username = userParts.length > 0 && !userParts[0].isEmpty() ? userParts[0] : null;
        String password = userParts.length > 1 ? userParts[1] : null;
        
        String hostDbPart = url.substring(atIndex + 1);
        int slashIndex = hostDbPart.indexOf('/');
        if (slashIndex <= 0) {
            return null;
        }
        
        String hostPort = hostDbPart.substring(0, slashIndex);
        String database = hostDbPart.substring(slashIndex + 1);
        
        if (database.isEmpty()) {
            return null;
        }
        
        int questionMarkIndex = database.indexOf('?');
        if (questionMarkIndex > 0) {
            database = database.substring(0, questionMarkIndex);
        }
        
        String host;
        int port = 5432;
        
        if (hostPort.contains(":")) {
            String[] hostPortParts = hostPort.split(":", 2);
            host = hostPortParts[0];
            if (hostPortParts.length > 1 && !hostPortParts[1].isEmpty()) {
                try {
                    port = Integer.parseInt(hostPortParts[1]);
                } catch (NumberFormatException e) {
                    port = 5432;
                }
            }
        } else {
            host = hostPort;
        }
        
        if (host == null || host.isEmpty()) {
            return null;
        }
        
        return new ParsedDatabaseUrl(host, port, database, username, password);
    }
    
    private static class ParsedDatabaseUrl {
        private final String host;
        private final int port;
        private final String database;
        private final String username;
        private final String password;
        
        ParsedDatabaseUrl(String host, int port, String database, String username, String password) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.username = username;
            this.password = password;
        }
        
        boolean isValid() {
            return host != null && !host.isEmpty() && database != null && !database.isEmpty();
        }
        
        String getJdbcUrl() {
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        }
        
        String getUsername() {
            return username;
        }
        
        String getPassword() {
            return password;
        }
    }
}

