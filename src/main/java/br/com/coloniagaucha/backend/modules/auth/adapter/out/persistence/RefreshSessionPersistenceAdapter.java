package br.com.coloniagaucha.backend.modules.auth.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.auth.application.port.RefreshSessionPort;
import br.com.coloniagaucha.backend.modules.auth.domain.RefreshSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class RefreshSessionPersistenceAdapter implements RefreshSessionPort {
    private final RefreshSessionJpaRepository repository;

    RefreshSessionPersistenceAdapter(RefreshSessionJpaRepository repository) { this.repository = repository; }

    @Override
    @Transactional
    public Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(RefreshSessionEntity::toDomain);
    }

    @Override
    @Transactional
    public void save(RefreshSession session) {
        repository.save(new RefreshSessionEntity(session));
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId, Instant at) {
        repository.revokeFamily(familyId, at);
    }
}
