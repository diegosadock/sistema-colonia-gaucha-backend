package br.com.coloniagaucha.backend.shared.domain;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " não encontrado: " + id);
    }
}
