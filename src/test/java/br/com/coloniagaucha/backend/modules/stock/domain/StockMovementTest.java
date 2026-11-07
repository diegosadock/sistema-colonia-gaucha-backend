package br.com.coloniagaucha.backend.modules.stock.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockMovementTest {
    private static final UUID PRODUCT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void entryAndExitUsePositiveQuantitiesWhileAdjustmentsUseSignedDeltas() {
        assertEquals(new BigDecimal("2.000"), create(MovementType.ENTRY, "2", null).delta());
        assertEquals(new BigDecimal("-2.000"), create(MovementType.EXIT, "2", null).delta());
        assertEquals(new BigDecimal("-0.500"), create(MovementType.ADJUSTMENT, "-0.5", "Inventário").delta());
    }

    @Test
    void adjustmentRequiresReasonAndNoMovementMayBeZero() {
        assertThrows(BusinessRuleException.class, () -> create(MovementType.ADJUSTMENT, "1", null));
        assertThrows(BusinessRuleException.class, () -> create(MovementType.ENTRY, "0", null));
        assertThrows(BusinessRuleException.class, () -> create(MovementType.EXIT, "-1", null));
    }

    private static StockMovement create(MovementType type, String quantity, String reason) {
        return StockMovement.create(PRODUCT, null, type, new BigDecimal(quantity), reason, "g", NOW, USER);
    }
}
