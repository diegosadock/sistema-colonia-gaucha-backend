package br.com.coloniagaucha.backend.modules.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.coloniagaucha.backend.modules.auth.application.port.AccessTokenIssuer;
import br.com.coloniagaucha.backend.modules.auth.application.port.AuthUserPort;
import br.com.coloniagaucha.backend.modules.auth.application.port.PasswordHasher;
import br.com.coloniagaucha.backend.modules.auth.application.port.RefreshSessionPort;
import br.com.coloniagaucha.backend.modules.auth.domain.InvalidAuthException;
import br.com.coloniagaucha.backend.modules.auth.domain.RefreshSession;
import br.com.coloniagaucha.backend.modules.auth.domain.Role;
import br.com.coloniagaucha.backend.modules.auth.domain.User;
import br.com.coloniagaucha.backend.shared.config.JwtProperties;
import br.com.coloniagaucha.backend.shared.config.RefreshProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthUseCasesTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    @Mock AuthUserPort users;
    @Mock RefreshSessionPort sessions;
    @Mock PasswordHasher passwords;
    @Mock AccessTokenIssuer tokenIssuer;
    private AuthUseCases useCases;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties jwt = new JwtProperties();
        RefreshProperties refresh = new RefreshProperties();
        useCases = new AuthUseCases(users, sessions, passwords, tokenIssuer, jwt, refresh,
                Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User(UUID.randomUUID(), "admin@example.com", "hash", true, Set.of(Role.ADMIN), NOW, NOW);
    }

    @Test
    void loginIssuesAccessTokenAndPersistsOnlyTheRefreshTokenHash() {
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("correct-password", "hash")).thenReturn(true);
        when(tokenIssuer.issue(user, NOW, Duration.ofMinutes(15)))
                .thenReturn(new AccessTokenIssuer.IssuedAccessToken("signed-access-token", 900));

        AuthUseCases.AuthTokens result = useCases.login(" Admin@Example.com ", "correct-password");

        assertEquals("signed-access-token", result.response().accessToken());
        assertTrue(result.refreshToken().length() >= 43);
        ArgumentCaptor<RefreshSession> stored = ArgumentCaptor.forClass(RefreshSession.class);
        verify(sessions).save(stored.capture());
        assertEquals(64, stored.getValue().tokenHash().length());
        assertNotEquals(result.refreshToken(), stored.getValue().tokenHash());
    }

    @Test
    void loginRejectsIncorrectPasswordWithoutCreatingSession() {
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("wrong", "hash")).thenReturn(false);
        assertThrows(InvalidAuthException.class, () -> useCases.login("admin@example.com", "wrong"));
        verify(sessions, never()).save(any());
    }

    @Test
    void refreshRotatesParentAndPersistsReplacement() {
        UUID family = UUID.randomUUID();
        RefreshSession current = RefreshSession.start(UUID.randomUUID(), user.id(), "old-hash", family, NOW, NOW.plusSeconds(600));
        when(sessions.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(tokenIssuer.issue(user, NOW, Duration.ofMinutes(15)))
                .thenReturn(new AccessTokenIssuer.IssuedAccessToken("new-access", 900));

        AuthUseCases.RefreshOutcome result = useCases.refresh("old-opaque-refresh");

        assertTrue(result.valid());
        assertTrue(current.isRotated());
        assertNotEquals("old-opaque-refresh", result.tokens().refreshToken());
        verify(sessions, org.mockito.Mockito.times(2)).save(any(RefreshSession.class));
    }

    @Test
    void reuseOfRotatedTokenRevokesTheWholeFamily() {
        UUID family = UUID.randomUUID();
        RefreshSession current = RefreshSession.start(UUID.randomUUID(), user.id(), "old-hash", family, NOW, NOW.plusSeconds(600));
        current.rotate(NOW.minusSeconds(1), UUID.randomUUID());
        when(sessions.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));

        AuthUseCases.RefreshOutcome result = useCases.refresh("replayed-token");

        assertFalse(result.valid());
        assertEquals(NOW, current.reuseDetectedAt());
        verify(sessions).revokeFamily(family, NOW);
    }
}
