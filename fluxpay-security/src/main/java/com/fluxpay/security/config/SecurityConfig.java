package com.fluxpay.security.config;

import com.fluxpay.security.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String corsAllowedOrigins;

    @Value("${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${CORS_ALLOWED_HEADERS:*}")
    private String corsAllowedHeaders;

    @Value("${CORS_EXPOSED_HEADERS:Authorization}")
    private String corsExposedHeaders;

    @Value("${CORS_ALLOW_CREDENTIALS:true}")
    private boolean corsAllowCredentials;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/tenants/register", "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (StringUtils.hasText(corsAllowedOrigins)) {
            List<String> origins = Arrays.asList(corsAllowedOrigins.split(","));
            configuration.setAllowedOrigins(origins.stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList());
            configuration.setAllowCredentials(corsAllowCredentials);
        } else {
            configuration.addAllowedOriginPattern("*");
            configuration.setAllowCredentials(false);
        }

        List<String> methods = Arrays.asList(corsAllowedMethods.split(","));
        configuration.setAllowedMethods(methods.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList());

        if ("*".equals(corsAllowedHeaders)) {
            configuration.addAllowedHeader("*");
        } else {
            List<String> headers = Arrays.asList(corsAllowedHeaders.split(","));
            configuration.setAllowedHeaders(headers.stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList());
        }

        List<String> exposedHeaders = Arrays.asList(corsExposedHeaders.split(","));
        configuration.setExposedHeaders(exposedHeaders.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

