package br.com.coloniagaucha.backend.modules.product.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.product.domain.Product;
import br.com.coloniagaucha.backend.modules.product.domain.UnitOfMeasure;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductJpaEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "name_key", nullable = false, unique = true, length = 360) private String nameKey;
    @Column(length = 255) private String description;
    @Column(length = 80) private String category;
    @Convert(converter = ProductUnitConverter.class)
    @Column(name = "unit_of_measure", nullable = false, length = 2) private UnitOfMeasure unitOfMeasure;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ProductJpaEntity() { }

    ProductJpaEntity(Product product) { updateFrom(product); }

    void updateFrom(Product product) {
        id = product.id();
        name = product.name();
        nameKey = product.nameKey();
        description = product.description();
        category = product.category();
        unitOfMeasure = product.unitOfMeasure();
        price = product.price();
        active = product.active();
        createdAt = product.createdAt();
        updatedAt = product.updatedAt();
    }

    Product toDomain() {
        return Product.reconstitute(id, name, description, category, unitOfMeasure, price, active, createdAt, updatedAt);
    }

    UUID getId() { return id; }
    UnitOfMeasure getUnitOfMeasure() { return unitOfMeasure; }
    boolean isActive() { return active; }
}
