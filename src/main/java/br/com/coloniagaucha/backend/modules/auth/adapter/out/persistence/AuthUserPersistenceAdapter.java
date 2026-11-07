package br.com.coloniagaucha.backend.modules.auth.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.auth.application.port.AuthUserPort;
import br.com.coloniagaucha.backend.modules.auth.domain.Role;
import br.com.coloniagaucha.backend.modules.auth.domain.User;
import java.time.Instant;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class AuthUserPersistenceAdapter implements AuthUserPort {
    private final AuthUserJpaRepository users;
    private final AuthRoleJpaRepository roles;
    private final Clock clock;

    AuthUserPersistenceAdapter(AuthUserJpaRepository users, AuthRoleJpaRepository roles, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return users.findByEmail(email).map(AuthUserPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return users.findById(id).map(AuthUserPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) { return users.existsByEmail(email); }

    @Override
    @Transactional(readOnly = true)
    public boolean adminExists() { return users.adminExists(); }

    @Override
    @Transactional
    public User save(String email, String passwordHash, boolean active, Set<Role> roleNames) {
        EnumSet<Role> required = roleNames.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roleNames);
        var roleEntities = roles.findByNameIn(required).stream().collect(Collectors.toSet());
        if (roleEntities.size() != required.size()) throw new IllegalStateException("Roles iniciais não foram carregadas pelo Flyway.");
        AuthUserEntity entity = new AuthUserEntity(UUID.randomUUID(), email, passwordHash, active, clock.instant(), roleEntities);
        return toDomain(users.save(entity));
    }

    private static User toDomain(AuthUserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.isActive(),
                entity.getRoles().stream().map(AuthRoleEntity::getName).collect(Collectors.toSet()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
