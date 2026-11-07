package br.com.coloniagaucha.backend.modules.auth.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.auth.domain.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuthRoleJpaRepository extends JpaRepository<AuthRoleEntity, Role> {
    List<AuthRoleEntity> findByNameIn(Collection<Role> names);
}
