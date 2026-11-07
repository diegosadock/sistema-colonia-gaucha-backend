package br.com.coloniagaucha.backend.modules.stock.adapter.out.persistence;

import br.com.coloniagaucha.backend.modules.stock.domain.MovementType;
import br.com.coloniagaucha.backend.modules.stock.domain.StockMovement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
public class StockMovementJpaEntity {
    @Id private UUID id;
    @Column(name = "product_id", nullable = false, updatable = false) private UUID productId;
    @Column(name = "supplier_id", updatable = false) private UUID supplierId;
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20, updatable = false) private MovementType type;
    @Column(nullable = false, precision = 18, scale = 3, updatable = false) private BigDecimal quantity;
    @Column(length = 255, updatable = false) private String reason;
    @Column(name = "unit_of_measure", nullable = false, length = 2, updatable = false) private String unitCode;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "created_by", nullable = false, updatable = false) private UUID createdBy;

    protected StockMovementJpaEntity() { }
    StockMovementJpaEntity(StockMovement movement) {
        id = movement.id(); productId = movement.productId(); supplierId = movement.supplierId();
        type = movement.type(); quantity = movement.quantity(); reason = movement.reason();
        unitCode = movement.unitCode(); createdAt = movement.createdAt(); createdBy = movement.createdBy();
    }
    StockMovement toDomain() {
        return new StockMovement(id, productId, supplierId, type, quantity, reason, unitCode, createdAt, createdBy);
    }
}
