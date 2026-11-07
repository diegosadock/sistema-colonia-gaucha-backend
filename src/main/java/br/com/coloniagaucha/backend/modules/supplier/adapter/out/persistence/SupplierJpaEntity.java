package br.com.coloniagaucha.backend.modules.supplier.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.supplier.domain.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "suppliers")
public class SupplierJpaEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 254) private String email;
    @Column(length = 32) private String phone;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected SupplierJpaEntity() { }
    SupplierJpaEntity(Supplier supplier) { updateFrom(supplier); }
    void updateFrom(Supplier supplier) {
        id = supplier.id(); name = supplier.name(); email = supplier.email(); phone = supplier.phone();
        active = supplier.active(); createdAt = supplier.createdAt(); updatedAt = supplier.updatedAt();
    }
    Supplier toDomain() { return Supplier.reconstitute(id, name, email, phone, active, createdAt, updatedAt); }
    UUID getId() { return id; }
    boolean isActive() { return active; }
}
