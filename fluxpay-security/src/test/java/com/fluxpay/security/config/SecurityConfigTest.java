package com.fluxpay.security.config;

import com.fluxpay.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtAuthenticationFilter);
    }

    @Test
    void testPasswordEncoder() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        
        assertNotNull(passwordEncoder);
        
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void testCorsConfigurationSource() {
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource();
        
        assertNotNull(corsConfigurationSource);
        assertTrue(corsConfigurationSource instanceof UrlBasedCorsConfigurationSource);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);
        
        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("*"));
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("PUT"));
        assertTrue(config.getAllowedMethods().contains("DELETE"));
        assertTrue(config.getAllowedMethods().contains("PATCH"));
        assertTrue(config.getAllowedMethods().contains("OPTIONS"));
        assertTrue(config.getAllowedHeaders().contains("*"));
        assertTrue(config.getExposedHeaders().contains("Authorization"));
    }

    @Test
    void testPasswordEncoderDifferentPasswords() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        
        String password1 = "password1";
        String password2 = "password2";
        
        String encoded1 = passwordEncoder.encode(password1);
        String encoded2 = passwordEncoder.encode(password2);
        
        assertNotEquals(encoded1, encoded2);
        assertTrue(passwordEncoder.matches(password1, encoded1));
        assertFalse(passwordEncoder.matches(password1, encoded2));
    }

    @Test
    void testPasswordEncoderSamePasswordDifferentEncodings() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        
        String password = "samePassword";
        String encoded1 = passwordEncoder.encode(password);
        String encoded2 = passwordEncoder.encode(password);
        
        assertNotEquals(encoded1, encoded2);
        assertTrue(passwordEncoder.matches(password, encoded1));
        assertTrue(passwordEncoder.matches(password, encoded2));
    }

    @Test
    void testPasswordEncoderWithNullPassword() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        
        assertThrows(Exception.class, () -> passwordEncoder.encode(null));
    }

    @Test
    void testPasswordEncoderWithEmptyPassword() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        
        String encoded = passwordEncoder.encode("");
        assertNotNull(encoded);
        assertTrue(passwordEncoder.matches("", encoded));
    }
}

