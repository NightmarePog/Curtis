package com.sosehl.curtis.feature.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media")
public class MediaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MediaEntity() {}

    public MediaEntity(
        UUID id,
        UUID ownerId,
        String storageKey,
        String originalName,
        String contentType,
        long byteSize,
        String sha256,
        Instant createdAt
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.contentType = contentType;
        this.byteSize = byteSize;
        this.sha256 = sha256;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getByteSize() { return byteSize; }
    public String getSha256() { return sha256; }
    public Instant getCreatedAt() { return createdAt; }
}
