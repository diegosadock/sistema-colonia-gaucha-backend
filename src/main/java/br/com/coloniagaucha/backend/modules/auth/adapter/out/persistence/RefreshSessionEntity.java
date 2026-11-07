package br.com.coloniagaucha.backend.modules.auth.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.auth.domain.RefreshSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSessionEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "family_id", nullable = false)
    private UUID familyId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "rotated_at")
    private Instant rotatedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "replaced_by_id")
    private UUID replacedById;
    @Column(name = "reuse_detected_at")
    private Instant reuseDetectedAt;

    protected RefreshSessionEntity() { }

    RefreshSessionEntity(RefreshSession session) { updateFrom(session); }

    void updateFrom(RefreshSession session) {
        id = session.id();
        userId = session.userId();
        tokenHash = session.tokenHash();
        familyId = session.familyId();
        createdAt = session.createdAt();
        expiresAt = session.expiresAt();
        rotatedAt = session.rotatedAt();
        revokedAt = session.revokedAt();
        replacedById = session.replacedById();
        reuseDetectedAt = session.reuseDetectedAt();
    }

    RefreshSession toDomain() {
        return new RefreshSession(id, userId, tokenHash, familyId, createdAt, expiresAt, rotatedAt, revokedAt, replacedById, reuseDetectedAt);
    }
}
