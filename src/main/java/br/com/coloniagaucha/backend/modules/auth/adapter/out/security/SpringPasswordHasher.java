package br.com.coloniagaucha.backend.modules.auth.adapter.out.security;

import br.com.coloniagaucha.backend.modules.auth.application.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
public class SpringPasswordHasher implements PasswordHasher {
    private final PasswordEncoder encoder;

    public SpringPasswordHasher(PasswordEncoder encoder) { this.encoder = encoder; }
    @Override public String hash(String rawPassword) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Senha excede o limite seguro do algoritmo configurado.");
        }
        return encoder.encode(rawPassword);
    }
    @Override public boolean matches(String rawPassword, String encodedPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length <= 72 && encoder.matches(rawPassword, encodedPassword);
    }
}
