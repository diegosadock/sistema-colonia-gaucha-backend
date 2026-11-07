package br.com.coloniagaucha.backend.modules.product.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void normalizesNameAndRetainsMoneyPrecision() {
        Product product = Product.create("  Café   moído ", null, " Mercearia ", UnitOfMeasure.GRAMA, new BigDecimal("12.30"), NOW);
        assertEquals("Café moído", product.name());
        assertEquals("café moído", product.nameKey());
        assertEquals("12.30", product.price().toPlainString());
        assertEquals("g", product.unitOfMeasure().code());
    }

    @Test
    void rejectsInvalidNamesAndPrices() {
        assertThrows(BusinessRuleException.class, () -> Product.create("  ", null, null, UnitOfMeasure.GRAMA, BigDecimal.ONE, NOW));
        assertThrows(BusinessRuleException.class, () -> Product.create("A", null, null, UnitOfMeasure.GRAMA, BigDecimal.ZERO, NOW));
        assertThrows(BusinessRuleException.class, () -> Product.create("A", null, null, UnitOfMeasure.GRAMA, new BigDecimal("1.001"), NOW));
    }

    @Test
    void deactivatesIdempotentlyAndReactivates() {
        Product product = Product.create("Sal", null, null, UnitOfMeasure.UNIDADE, BigDecimal.ONE, NOW);
        product.deactivate(NOW.plusSeconds(1));
        assertFalse(product.active());
        Instant updatedAt = product.updatedAt();
        product.deactivate(NOW.plusSeconds(2));
        assertEquals(updatedAt, product.updatedAt());
        product.reactivate(NOW.plusSeconds(3));
        assertTrue(product.active());
    }
}
