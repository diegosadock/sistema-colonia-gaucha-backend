package br.com.coloniagaucha.backend.modules.stock.domain;

import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.util.UUID;

public record StockMovement(
        UUID id,
        UUID productId,
        UUID supplierId,
        MovementType type,
        BigDecimal quantity,
        String reason,
        String unitCode,
        Instant createdAt,
        UUID createdBy) {
    public StockMovement {
        if (id == null || productId == null || type == null || createdAt == null || createdBy == null) {
            throw new BusinessRuleException("Identificadores, tipo e data do movimento são obrigatórios.");
        }
        if (quantity == null || quantity.signum() == 0) throw new BusinessRuleException("Quantidade do movimento não pode ser zero.");
        try {
            quantity = quantity.setScale(3, RoundingMode.UNNECESSARY);
            if (quantity.precision() > 18) throw new ArithmeticException();
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("Quantidade deve caber em NUMERIC(18,3).");
        }
        if (type == MovementType.ENTRY || type == MovementType.EXIT) {
            if (quantity.signum() < 0) throw new BusinessRuleException("Entrada e saída recebem quantidade positiva.");
        }
        if (type == MovementType.ADJUSTMENT && (reason == null || reason.isBlank())) {
            throw new BusinessRuleException("Ajuste de estoque exige justificativa.");
        }
        if (type != MovementType.ADJUSTMENT && quantity.signum() < 0) {
            throw new BusinessRuleException("Somente ajuste pode receber quantidade negativa.");
        }
        if (supplierId != null && type != MovementType.ENTRY) {
            throw new BusinessRuleException("Fornecedor pode ser informado somente em entradas.");
        }
        if (unitCode == null || !java.util.Set.of("g", "ml", "un").contains(unitCode)) {
            throw new BusinessRuleException("Unidade de medida de estoque inválida.");
        }
        if (reason != null) {
            reason = Normalizer.normalize(reason, Normalizer.Form.NFKC).strip();
            if (reason.isEmpty()) reason = null;
            if (reason != null && reason.length() > 255) throw new BusinessRuleException("Justificativa deve ter até 255 caracteres.");
        }
    }

    public BigDecimal delta() { return type == MovementType.EXIT ? quantity.negate() : quantity; }

    public static StockMovement create(UUID productId, UUID supplierId, MovementType type, BigDecimal quantity,
                                       String reason, String unitCode, Instant now, UUID createdBy) {
        return new StockMovement(UUID.randomUUID(), productId, supplierId, type, quantity, reason, unitCode, now, createdBy);
    }
}
