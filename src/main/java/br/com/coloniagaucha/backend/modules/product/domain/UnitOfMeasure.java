package br.com.coloniagaucha.backend.modules.product.domain;

import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;
import java.util.Arrays;

public enum UnitOfMeasure {
    GRAMA("g"),
    MILILITRO("ml"),
    UNIDADE("un");

    private final String code;
    UnitOfMeasure(String code) { this.code = code; }
    public String code() { return code; }

    public static UnitOfMeasure fromCode(String code) {
        return Arrays.stream(values()).filter(unit -> unit.code.equals(code)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("Unidade de medida deve ser g, ml ou un."));
    }
}
