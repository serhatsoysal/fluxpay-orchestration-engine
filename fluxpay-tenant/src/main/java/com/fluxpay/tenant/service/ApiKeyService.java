package com.fluxpay.tenant.service;

import com.fluxpay.common.exception.ResourceNotFoundException;
import com.fluxpay.tenant.entity.ApiKey;
import com.fluxpay.tenant.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApiKeyService {

    private static final String KEY_PREFIX_LIVE = "fpk_live_";
    private static final String KEY_PREFIX_TEST = "fpk_test_";
    private static final int KEY_LENGTH = 32;

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public ApiKey createApiKey(ApiKey apiKey, boolean isLive) {
        String prefix = isLive ? KEY_PREFIX_LIVE : KEY_PREFIX_TEST;
        String key = generateApiKey(prefix);
        
        apiKey.setKeyPrefix(prefix);
        apiKey.setKeyHash(hashKey(key));
        
        return apiKeyRepository.save(apiKey);
    }

    @Transactional(readOnly = true)
    public ApiKey getApiKeyById(UUID id) {
        return apiKeyRepository.findById(id)
                .filter(k -> !k.isRevoked())
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
    }

    @Transactional(readOnly = true)
    public List<ApiKey> getActiveApiKeysByTenant(UUID tenantId) {
        return apiKeyRepository.findByTenantIdAndRevokedAtIsNull(tenantId);
    }

    @Transactional(readOnly = true)
    public ApiKey validateApiKey(String keyHash) {
        return apiKeyRepository.findByKeyHash(keyHash)
                .filter(k -> !k.isRevoked() && !k.isExpired())
                .orElseThrow(() -> new ResourceNotFoundException("Valid ApiKey not found"));
    }

    public void revokeApiKey(UUID id) {
        ApiKey apiKey = getApiKeyById(id);
        apiKey.setRevokedAt(Instant.now());
        apiKeyRepository.save(apiKey);
    }

    public void updateLastUsed(UUID id) {
        ApiKey apiKey = getApiKeyById(id);
        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);
    }

    private String generateApiKey(String prefix) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[KEY_LENGTH];
        random.nextBytes(bytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return prefix + randomPart;
    }

    private String hashKey(String key) {
        return Integer.toHexString(key.hashCode());
    }
}

