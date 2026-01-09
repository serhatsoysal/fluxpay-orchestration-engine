package com.fluxpay.tenant.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.tenant.entity.ApiKey;
import com.fluxpay.tenant.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private ApiKey apiKey;
    private UUID apiKeyId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        apiKeyId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        apiKey = new ApiKey();
        apiKey.setId(apiKeyId);
        apiKey.setTenantId(tenantId);
    }

    @Test
    void createApiKey_WithLiveKey_ShouldSucceed() {
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

        ApiKey result = apiKeyService.createApiKey(apiKey, true);

        assertThat(result).isNotNull();
        assertThat(result.getKeyPrefix()).isEqualTo("fpk_live_");
        assertThat(result.getKeyHash()).isNotNull();
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void createApiKey_WithTestKey_ShouldSucceed() {
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

        ApiKey result = apiKeyService.createApiKey(apiKey, false);

        assertThat(result).isNotNull();
        assertThat(result.getKeyPrefix()).isEqualTo("fpk_test_");
        assertThat(result.getKeyHash()).isNotNull();
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void getApiKeyById_ShouldReturnApiKey() {
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

        ApiKey result = apiKeyService.getApiKeyById(apiKeyId);

        assertThat(result).isEqualTo(apiKey);
        verify(apiKeyRepository).findById(apiKeyId);
    }

    @Test
    void getApiKeyById_ShouldThrowException_WhenRevoked() {
        apiKey.setRevokedAt(Instant.now());
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

        assertThatThrownBy(() -> apiKeyService.getApiKeyById(apiKeyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActiveApiKeysByTenant_ShouldReturnList() {
        when(apiKeyRepository.findByTenantIdAndRevokedAtIsNull(tenantId))
                .thenReturn(List.of(apiKey));

        List<ApiKey> result = apiKeyService.getActiveApiKeysByTenant(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(apiKey);
    }

    @Test
    void validateApiKey_ShouldReturnApiKey_WhenValid() {
        apiKey.setKeyHash("validHash");
        apiKey.setExpiresAt(Instant.now().plusSeconds(3600));
        when(apiKeyRepository.findByKeyHash("validHash")).thenReturn(Optional.of(apiKey));

        ApiKey result = apiKeyService.validateApiKey("validHash");

        assertThat(result).isEqualTo(apiKey);
    }

    @Test
    void validateApiKey_ShouldThrowException_WhenExpired() {
        apiKey.setKeyHash("expiredHash");
        apiKey.setExpiresAt(Instant.now().minusSeconds(3600));
        when(apiKeyRepository.findByKeyHash("expiredHash")).thenReturn(Optional.of(apiKey));

        assertThatThrownBy(() -> apiKeyService.validateApiKey("expiredHash"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void revokeApiKey_ShouldSucceed() {
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

        apiKeyService.revokeApiKey(apiKeyId);

        assertThat(apiKey.getRevokedAt()).isNotNull();
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void updateLastUsed_ShouldSucceed() {
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(apiKey);

        apiKeyService.updateLastUsed(apiKeyId);

        assertThat(apiKey.getLastUsedAt()).isNotNull();
        verify(apiKeyRepository).save(apiKey);
    }
}

