package com.fluxpay.security.config;

import com.fluxpay.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private SecurityConfig securityConfig;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() throws Exception {
        securityConfig = new SecurityConfig(jwtAuthenticationFilter);
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = SecurityConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(securityConfig, value);
    }

    @Test
    void passwordEncoder_ReturnsBCryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder).isNotNull();
        assertThat(encoder).isInstanceOf(org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class);
    }

    @Test
    void corsConfigurationSource_ReturnsUrlBasedCorsConfigurationSource() throws Exception {
        setField("corsAllowedMethods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        setField("corsAllowedHeaders", "*");
        setField("corsExposedHeaders", "Authorization");

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config).isNotNull();
    }

    @Test
    void corsConfigurationSource_WithOrigins_ReturnsConfigurationWithOrigins() throws Exception {
        setField("corsAllowedOrigins", "https://example.com,https://test.com");
        setField("corsAllowedMethods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        setField("corsAllowedHeaders", "*");
        setField("corsExposedHeaders", "Authorization");
        setField("corsAllowCredentials", true);

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getAllowedOrigins()).containsExactly("https://example.com", "https://test.com");
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void corsConfigurationSource_WithoutOrigins_UsesWildcard() throws Exception {
        setField("corsAllowedOrigins", "");
        setField("corsAllowedMethods", "GET,POST");
        setField("corsAllowedHeaders", "Content-Type");
        setField("corsExposedHeaders", "Authorization");

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getAllowedOriginPatterns()).contains("*");
        assertThat(config.getAllowCredentials()).isFalse();
    }

    @Test
    void corsConfigurationSource_WithNullMethods_UsesDefaults() throws Exception {
        setField("corsAllowedOrigins", "");
        setField("corsAllowedMethods", null);
        setField("corsAllowedHeaders", "*");
        setField("corsExposedHeaders", "Authorization");

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "*")
    void corsConfigurationSource_WithWildcardOrNullHeaders_SetsWildcard(String headersValue) throws Exception {
        setField("corsAllowedOrigins", "");
        setField("corsAllowedMethods", "GET,POST");
        setField("corsAllowedHeaders", headersValue);
        setField("corsExposedHeaders", "Authorization");

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getAllowedHeaders()).contains("*");
    }

    @Test
    void corsConfigurationSource_WithNullExposedHeaders_UsesDefault() throws Exception {
        setField("corsAllowedOrigins", "");
        setField("corsAllowedMethods", "GET,POST");
        setField("corsAllowedHeaders", "*");
        setField("corsExposedHeaders", null);

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getExposedHeaders()).contains("Authorization");
    }

    @Test
    void corsConfigurationSource_WithSpecificHeaders_ConfiguresCorrectly() throws Exception {
        setField("corsAllowedOrigins", "");
        setField("corsAllowedMethods", "GET,POST");
        setField("corsAllowedHeaders", "Content-Type,Authorization");
        setField("corsExposedHeaders", "Authorization,X-Custom");

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getAllowedHeaders()).containsExactly("Content-Type", "Authorization");
        assertThat(config.getExposedHeaders()).containsExactly("Authorization", "X-Custom");
    }

    @Test
    void passwordEncoder_ShouldEncryptPasswordsCorrectly() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "mySecurePassword123";
        
        String encodedPassword = encoder.encode(rawPassword);
        
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(encoder.matches("wrongPassword", encodedPassword)).isFalse();
    }

    @Test
    void corsConfigurationSource_WithEmptyMethodString_UsesDefaults() throws Exception {
        setField("corsAllowedOrigins", "");
        setField("corsAllowedMethods", "");
        setField("corsAllowedHeaders", "*");
        setField("corsExposedHeaders", "Authorization");

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }

    @Test
    void corsConfigurationSource_WithEmptyHeaderString_UsesWildcard() throws Exception {
        setField("corsAllowedOrigins", "");
        setField("corsAllowedMethods", "GET,POST");
        setField("corsAllowedHeaders", "");
        setField("corsExposedHeaders", "Authorization");

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getAllowedHeaders()).contains("*");
    }

    @Test
    void corsConfigurationSource_WithEmptyExposedHeaders_UsesDefault() throws Exception {
        setField("corsAllowedOrigins", "");
        setField("corsAllowedMethods", "GET,POST");
        setField("corsAllowedHeaders", "*");
        setField("corsExposedHeaders", "");

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getExposedHeaders()).contains("Authorization");
    }

    @Test
    void corsConfigurationSource_WithOriginsContainingSpaces_ShouldTrimSpaces() throws Exception {
        setField("corsAllowedOrigins", " https://example.com , https://test.com ");
        setField("corsAllowedMethods", "GET,POST");
        setField("corsAllowedHeaders", "*");
        setField("corsExposedHeaders", "Authorization");
        setField("corsAllowCredentials", true);

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getAllowedOrigins()).containsExactly("https://example.com", "https://test.com");
    }

    @Test
    void corsConfigurationSource_WithMethodsContainingSpaces_ShouldTrimSpaces() throws Exception {
        setField("corsAllowedOrigins", "");
        setField("corsAllowedMethods", " GET , POST , PUT ");
        setField("corsAllowedHeaders", "*");
        setField("corsExposedHeaders", "Authorization");

        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        assertThat(corsSource).isNotNull();
        CorsConfiguration config = corsSource.getCorsConfiguration(request);
        assertThat(config.getAllowedMethods()).containsExactly("GET", "POST", "PUT");
    }
}
