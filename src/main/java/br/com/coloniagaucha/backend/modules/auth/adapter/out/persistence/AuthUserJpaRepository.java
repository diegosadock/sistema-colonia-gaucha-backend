package br.com.coloniagaucha.backend.modules.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AuthUserJpaRepository extends JpaRepository<AuthUserEntity, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<AuthUserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select case when count(u) > 0 then true else false end from AuthUserEntity u join u.roles role where role.name = br.com.coloniagaucha.backend.modules.auth.domain.Role.ADMIN")
    boolean adminExists();
}
