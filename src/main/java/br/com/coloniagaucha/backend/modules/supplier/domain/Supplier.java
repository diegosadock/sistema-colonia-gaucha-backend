package br.com.coloniagaucha.backend.modules.supplier.domain;

import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public final class Supplier {
    private final UUID id;
    private String name;
    private String email;
    private String phone;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Supplier(UUID id, String name, String email, String phone, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = normalizeName(name);
        this.email = normalizeEmail(email);
        this.phone = normalizePhone(phone);
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Supplier create(String name, String email, String phone, Instant now) {
        return new Supplier(UUID.randomUUID(), name, email, phone, true, now, now);
    }

    public static Supplier reconstitute(UUID id, String name, String email, String phone,
                                        boolean active, Instant createdAt, Instant updatedAt) {
        return new Supplier(id, name, email, phone, active, createdAt, updatedAt);
    }

    public void update(String name, String email, String phone, Instant now) {
        if (name != null) this.name = normalizeName(name);
        if (email != null) this.email = normalizeEmail(email);
        if (phone != null) this.phone = normalizePhone(phone);
        updatedAt = now;
    }

    public void setActive(boolean active, Instant now) {
        if (this.active != active) { this.active = active; updatedAt = now; }
    }

    private static String normalizeName(String value) {
        if (value == null) throw new BusinessRuleException("Nome do fornecedor é obrigatório.");
        String name = Normalizer.normalize(value, Normalizer.Form.NFKC).replaceAll("(?U)\\s+", " ").strip();
        if (name.isBlank() || name.codePointCount(0, name.length()) > 150) throw new BusinessRuleException("Nome do fornecedor deve ter entre 1 e 150 caracteres.");
        return name;
    }

    private static String normalizeEmail(String value) {
        if (value == null || value.isBlank()) return null;
        String email = value.strip().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new BusinessRuleException("E-mail do fornecedor inválido.");
        return email;
    }

    private static String normalizePhone(String value) {
        if (value == null || value.isBlank()) return null;
        String phone = value.strip();
        if (phone.length() > 32 || !phone.matches("^[+0-9() .-]+$")) throw new BusinessRuleException("Telefone do fornecedor inválido.");
        return phone;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String email() { return email; }
    public String phone() { return phone; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
