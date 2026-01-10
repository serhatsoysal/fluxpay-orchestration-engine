package com.fluxpay.common.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    private static class TestEntity extends BaseEntity {
    }

    @Test
    void testIsDeleted_WithNullDeletedAt_ShouldReturnFalse() {
        TestEntity entity = new TestEntity();
        
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void testIsDeleted_WithDeletedAt_ShouldReturnTrue() {
        TestEntity entity = new TestEntity();
        entity.setDeletedAt(Instant.now());
        
        assertThat(entity.isDeleted()).isTrue();
    }

    @Test
    void testSoftDelete_ShouldSetDeletedAt() {
        TestEntity entity = new TestEntity();
        Instant before = Instant.now();
        
        entity.softDelete();
        
        assertThat(entity.getDeletedAt()).isNotNull();
        assertThat(entity.getDeletedAt()).isAfterOrEqualTo(before);
        assertThat(entity.isDeleted()).isTrue();
    }

    @Test
    void testGettersAndSetters() {
        TestEntity entity = new TestEntity();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        entity.setId(id);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeletedAt(now);
        
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getDeletedAt()).isEqualTo(now);
    }
}

