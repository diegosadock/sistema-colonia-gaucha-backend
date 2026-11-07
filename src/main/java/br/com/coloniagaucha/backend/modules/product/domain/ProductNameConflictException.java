package br.com.coloniagaucha.backend.modules.product.domain;

import br.com.coloniagaucha.backend.shared.domain.ResourceConflictException;

public class ProductNameConflictException extends ResourceConflictException {
    public ProductNameConflictException() { super("Já existe um produto com esse nome."); }
}
