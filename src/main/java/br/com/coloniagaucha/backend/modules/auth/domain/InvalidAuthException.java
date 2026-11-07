package br.com.coloniagaucha.backend.modules.auth.domain;

public class InvalidAuthException extends RuntimeException {
    public InvalidAuthException() {
        super("Credenciais ou sessão inválidas.");
    }
}
