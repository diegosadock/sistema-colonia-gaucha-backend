package br.com.coloniagaucha.backend.shared.application;

import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;

public record PageQuery(int page, int size, String sort, Direction direction) {
    public PageQuery {
        if (page < 0) throw new BusinessRuleException("page deve ser zero ou maior.");
        if (size < 1 || size > 100) throw new BusinessRuleException("size deve estar entre 1 e 100.");
        sort = sort == null || sort.isBlank() ? "createdAt" : sort;
        direction = direction == null ? Direction.DESC : direction;
    }

    public enum Direction { ASC, DESC }
}
