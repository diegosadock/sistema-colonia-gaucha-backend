package br.com.coloniagaucha.backend.modules.auth.application.port;

import br.com.coloniagaucha.backend.modules.auth.domain.RefreshSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionPort {
    Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash);
    void save(RefreshSession session);
    void revokeFamily(UUID familyId, Instant at);
}
