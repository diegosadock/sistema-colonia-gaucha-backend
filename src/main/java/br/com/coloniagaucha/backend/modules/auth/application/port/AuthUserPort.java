package br.com.coloniagaucha.backend.modules.auth.application.port;

import br.com.coloniagaucha.backend.modules.auth.domain.Role;
import br.com.coloniagaucha.backend.modules.auth.domain.User;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AuthUserPort {
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    boolean existsByEmail(String email);
    boolean adminExists();
    User save(String email, String passwordHash, boolean active, Set<Role> roles);
}
