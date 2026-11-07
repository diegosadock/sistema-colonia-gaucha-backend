package br.com.coloniagaucha.backend.shared.domain;

public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
}
