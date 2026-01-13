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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        apiKey.setKeyPrefix("fpk_live_");
        apiKey.setKeyHash("hashed-key");
        apiKey.setRevokedAt(null);
        apiKey.setExpiresAt(Instant.now().plusSeconds(86400));
    }

    @Test
    void createApiKey_WithLiveKey_ShouldSetLivePrefix() {
        ApiKey newApiKey = new ApiKey();
        newApiKey.setTenantId(tenantId);

        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey key = invocation.getArgument(0);
            key.setId(UUID.randomUUID());
            return key;
        });

        ApiKey result = apiKeyService.createApiKey(newApiKey, true);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getKeyPrefix()).isEqualTo("fpk_live_");
        assertThat(result.getKeyHash()).isNotNull();
        verify(apiKeyRepository).save(newApiKey);
    }

    @Test
    void createApiKey_WithTestKey_ShouldSetTestPrefix() {
        ApiKey newApiKey = new ApiKey();
        newApiKey.setTenantId(tenantId);

        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey key = invocation.getArgument(0);
            key.setId(UUID.randomUUID());
            return key;
        });

        ApiKey result = apiKeyService.createApiKey(newApiKey, false);

        assertThat(result).isNotNull();
        assertThat(result.getKeyPrefix()).isEqualTo("fpk_test_");
        assertThat(result.getKeyHash()).isNotNull();
        verify(apiKeyRepository).save(newApiKey);
    }

    @Test
    void getApiKeyById_ShouldReturnApiKey() {
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

        ApiKey result = apiKeyService.getApiKeyById(apiKeyId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(apiKeyId);
        verify(apiKeyRepository).findById(apiKeyId);
    }

    @Test
    void getApiKeyById_WhenNotFound_ShouldThrowException() {
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.getApiKeyById(apiKeyId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(apiKeyRepository).findById(apiKeyId);
    }

    @Test
    void getApiKeyById_WhenRevoked_ShouldThrowException() {
        apiKey.setRevokedAt(Instant.now());
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

        assertThatThrownBy(() -> apiKeyService.getApiKeyById(apiKeyId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(apiKeyRepository).findById(apiKeyId);
    }

    @Test
    void getActiveApiKeysByTenant_ShouldReturnActiveKeys() {
        ApiKey activeKey1 = new ApiKey();
        activeKey1.setId(UUID.randomUUID());
        activeKey1.setRevokedAt(null);
        ApiKey activeKey2 = new ApiKey();
        activeKey2.setId(UUID.randomUUID());
        activeKey2.setRevokedAt(null);

        List<ApiKey> activeKeys = Arrays.asList(activeKey1, activeKey2);

        when(apiKeyRepository.findByTenantIdAndRevokedAtIsNull(tenantId)).thenReturn(activeKeys);

        List<ApiKey> result = apiKeyService.getActiveApiKeysByTenant(tenantId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(key -> key.getRevokedAt() == null);
        verify(apiKeyRepository).findByTenantIdAndRevokedAtIsNull(tenantId);
    }

    @Test
    void getActiveApiKeysByTenant_ShouldReturnEmptyList() {
        when(apiKeyRepository.findByTenantIdAndRevokedAtIsNull(tenantId)).thenReturn(List.of());

        List<ApiKey> result = apiKeyService.getActiveApiKeysByTenant(tenantId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(apiKeyRepository).findByTenantIdAndRevokedAtIsNull(tenantId);
    }

    @Test
    void validateApiKey_WithValidKey_ShouldReturnApiKey() {
        String keyHash = "hashed-key";

        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(apiKey));

        ApiKey result = apiKeyService.validateApiKey(keyHash);

        assertThat(result).isNotNull();
        assertThat(result.getKeyHash()).isEqualTo(keyHash);
        verify(apiKeyRepository).findByKeyHash(keyHash);
    }

    @Test
    void validateApiKey_WhenNotFound_ShouldThrowException() {
        String keyHash = "nonexistent-hash";

        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.validateApiKey(keyHash))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(apiKeyRepository).findByKeyHash(keyHash);
    }

    @Test
    void validateApiKey_WhenRevoked_ShouldThrowException() {
        String keyHash = "hashed-key";
        apiKey.setRevokedAt(Instant.now());

        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(apiKey));

        assertThatThrownBy(() -> apiKeyService.validateApiKey(keyHash))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(apiKeyRepository).findByKeyHash(keyHash);
    }

    @Test
    void validateApiKey_WhenExpired_ShouldThrowException() {
        String keyHash = "hashed-key";
        apiKey.setExpiresAt(Instant.now().minusSeconds(3600));

        when(apiKeyRepository.findByKeyHash(keyHash)).thenReturn(Optional.of(apiKey));

        assertThatThrownBy(() -> apiKeyService.validateApiKey(keyHash))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(apiKeyRepository).findByKeyHash(keyHash);
    }

    @Test
    void revokeApiKey_ShouldSetRevokedAt() {
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        apiKeyService.revokeApiKey(apiKeyId);

        assertThat(apiKey.getRevokedAt()).isNotNull();
        verify(apiKeyRepository).findById(apiKeyId);
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void revokeApiKey_WhenNotFound_ShouldThrowException() {
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.revokeApiKey(apiKeyId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void updateLastUsed_ShouldSetLastUsedAt() {
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        apiKeyService.updateLastUsed(apiKeyId);

        assertThat(apiKey.getLastUsedAt()).isNotNull();
        verify(apiKeyRepository).findById(apiKeyId);
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void updateLastUsed_WhenNotFound_ShouldThrowException() {
        when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.updateLastUsed(apiKeyId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(apiKeyRepository, never()).save(any());
    }
}

