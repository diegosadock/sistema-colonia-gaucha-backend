package br.com.coloniagaucha.backend.modules.auth.domain;

import java.time.Instant;
import java.util.UUID;

public final class RefreshSession {
    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final UUID familyId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant rotatedAt;
    private Instant revokedAt;
    private UUID replacedById;
    private Instant reuseDetectedAt;

    public RefreshSession(
            UUID id,
            UUID userId,
            String tokenHash,
            UUID familyId,
            Instant createdAt,
            Instant expiresAt,
            Instant rotatedAt,
            Instant revokedAt,
            UUID replacedById,
            Instant reuseDetectedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.rotatedAt = rotatedAt;
        this.revokedAt = revokedAt;
        this.replacedById = replacedById;
        this.reuseDetectedAt = reuseDetectedAt;
    }

    public static RefreshSession start(UUID id, UUID userId, String tokenHash, UUID familyId, Instant now, Instant expiresAt) {
        return new RefreshSession(id, userId, tokenHash, familyId, now, expiresAt, null, null, null, null);
    }

    public void rotate(Instant now, UUID replacementId) {
        if (rotatedAt != null || revokedAt != null) throw new IllegalStateException("Sessão de refresh não pode ser rotacionada.");
        rotatedAt = now;
        replacedById = replacementId;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) revokedAt = now;
    }

    public void detectReuse(Instant now) {
        if (reuseDetectedAt == null) reuseDetectedAt = now;
        revoke(now);
    }

    public boolean isRotated() { return rotatedAt != null; }
    public boolean isRevoked() { return revokedAt != null; }
    public boolean isExpiredAt(Instant now) { return !expiresAt.isAfter(now); }
    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String tokenHash() { return tokenHash; }
    public UUID familyId() { return familyId; }
    public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; }
    public Instant rotatedAt() { return rotatedAt; }
    public Instant revokedAt() { return revokedAt; }
    public UUID replacedById() { return replacedById; }
    public Instant reuseDetectedAt() { return reuseDetectedAt; }
}
