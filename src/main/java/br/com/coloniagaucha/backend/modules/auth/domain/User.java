package br.com.coloniagaucha.backend.modules.auth.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash,
        boolean active,
        Set<Role> roles,
        Instant createdAt,
        Instant updatedAt) {
    public User {
        roles = Set.copyOf(roles);
    }
}
