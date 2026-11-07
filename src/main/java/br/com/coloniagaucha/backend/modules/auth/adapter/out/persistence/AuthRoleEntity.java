package br.com.coloniagaucha.backend.modules.auth.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.auth.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class AuthRoleEntity {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "name", length = 20)
    private Role name;

    protected AuthRoleEntity() { }

    public Role getName() { return name; }
}
