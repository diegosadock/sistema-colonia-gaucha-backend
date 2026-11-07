package br.com.coloniagaucha.backend.modules.product.domain;

import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class Product {
    private final UUID id;
    private String name;
    private String nameKey;
    private String description;
    private String category;
    private UnitOfMeasure unitOfMeasure;
    private BigDecimal price;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Product(UUID id, String name, String description, String category, UnitOfMeasure unitOfMeasure,
                    BigDecimal price, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = normalizeName(name);
        this.nameKey = nameKey(this.name);
        this.description = optionalText(description, "description", 255);
        this.category = optionalText(category, "category", 80);
        this.unitOfMeasure = requireUnit(unitOfMeasure);
        this.price = requirePrice(price);
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Product create(String name, String description, String category,
                                 UnitOfMeasure unitOfMeasure, BigDecimal price, Instant now) {
        return new Product(UUID.randomUUID(), name, description, category, unitOfMeasure, price, true, now, now);
    }

    public static Product reconstitute(UUID id, String name, String description, String category,
                                       UnitOfMeasure unitOfMeasure, BigDecimal price, boolean active,
                                       Instant createdAt, Instant updatedAt) {
        return new Product(id, name, description, category, unitOfMeasure, price, active, createdAt, updatedAt);
    }

    public void rename(String name, Instant now) {
        this.name = normalizeName(name);
        this.nameKey = nameKey(this.name);
        touch(now);
    }

    public void changeDescription(String description, Instant now) {
        this.description = optionalText(description, "description", 255);
        touch(now);
    }

    public void changeCategory(String category, Instant now) {
        this.category = optionalText(category, "category", 80);
        touch(now);
    }

    public void changeUnitOfMeasure(UnitOfMeasure unit, Instant now) {
        this.unitOfMeasure = requireUnit(unit);
        touch(now);
    }

    public void changePrice(BigDecimal price, Instant now) {
        this.price = requirePrice(price);
        touch(now);
    }

    public void deactivate(Instant now) {
        if (active) { active = false; touch(now); }
    }

    public void reactivate(Instant now) {
        if (!active) { active = true; touch(now); }
    }

    public static String normalizeName(String name) {
        if (name == null) throw new BusinessRuleException("Nome do produto é obrigatório.");
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFKC).replaceAll("(?U)\\s+", " ").strip();
        if (normalized.isEmpty()) throw new BusinessRuleException("Nome do produto é obrigatório.");
        if (normalized.codePointCount(0, normalized.length()) > 120) {
            throw new BusinessRuleException("Nome do produto deve ter até 120 caracteres.");
        }
        return normalized;
    }

    public static String nameKey(String name) {
        return Normalizer.normalize(normalizeName(name), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static String optionalText(String value, String field, int max) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).strip();
        if (normalized.isEmpty()) return null;
        if (normalized.codePointCount(0, normalized.length()) > max) {
            throw new BusinessRuleException(field + " deve ter até " + max + " caracteres.");
        }
        return normalized;
    }

    private static UnitOfMeasure requireUnit(UnitOfMeasure unit) {
        if (unit == null) throw new BusinessRuleException("Unidade de medida é obrigatória.");
        return unit;
    }

    private static BigDecimal requirePrice(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw new BusinessRuleException("Preço deve ser maior que zero.");
        try {
            BigDecimal normalized = value.setScale(2, RoundingMode.UNNECESSARY);
            if (normalized.precision() > 12) throw new ArithmeticException();
            return normalized;
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("Preço deve caber em NUMERIC(12,2).");
        }
    }

    private void touch(Instant now) { updatedAt = now; }
    public UUID id() { return id; }
    public String name() { return name; }
    public String nameKey() { return nameKey; }
    public String description() { return description; }
    public String category() { return category; }
    public UnitOfMeasure unitOfMeasure() { return unitOfMeasure; }
    public BigDecimal price() { return price; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
