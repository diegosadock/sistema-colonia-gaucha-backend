package br.com.coloniagaucha.backend.modules.auth.application.port;

import br.com.coloniagaucha.backend.modules.auth.domain.User;
import java.time.Instant;
import java.time.Duration;

public interface AccessTokenIssuer {
    IssuedAccessToken issue(User user, Instant now, Duration lifetime);

    record IssuedAccessToken(String value, long expiresInSeconds) { }
}
