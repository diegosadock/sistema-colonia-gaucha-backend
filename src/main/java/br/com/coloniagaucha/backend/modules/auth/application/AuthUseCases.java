package br.com.coloniagaucha.backend.modules.auth.application;

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
import br.com.coloniagaucha.backend.shared.domain.ResourceNotFoundException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthUseCases {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthUserPort users;
    private final RefreshSessionPort sessions;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final JwtProperties jwtProperties;
    private final RefreshProperties refreshProperties;
    private final Clock clock;

    public AuthUseCases(
            AuthUserPort users,
            RefreshSessionPort sessions,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer,
            JwtProperties jwtProperties,
            RefreshProperties refreshProperties,
            Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.jwtProperties = jwtProperties;
        this.refreshProperties = refreshProperties;
        this.clock = clock;
    }

    @Transactional
    public AuthTokens login(String email, String rawPassword) {
        User user = users.findByEmail(normalizeEmail(email))
                .filter(User::active)
                .filter(candidate -> passwordHasher.matches(rawPassword, candidate.passwordHash()))
                .orElseThrow(InvalidAuthException::new);
        return startSession(user, UUID.randomUUID());
    }

    @Transactional
    public RefreshOutcome refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return RefreshOutcome.failed();
        Instant now = clock.instant();
        RefreshSession current = sessions.findByTokenHashForUpdate(hashToken(rawRefreshToken)).orElse(null);
        if (current == null) return RefreshOutcome.failed();
        if (current.isRotated()) {
            current.detectReuse(now);
            sessions.save(current);
            sessions.revokeFamily(current.familyId(), now);
            return RefreshOutcome.failed();
        }
        if (current.isRevoked() || current.isExpiredAt(now)) return RefreshOutcome.failed();

        User user = users.findById(current.userId()).filter(User::active).orElse(null);
        if (user == null) {
            sessions.revokeFamily(current.familyId(), now);
            return RefreshOutcome.failed();
        }
        UUID replacementId = UUID.randomUUID();
        String replacementToken = newRefreshToken();
        Instant expiresAt = now.plus(refreshProperties.getTokenTtl());
        current.rotate(now, replacementId);
        sessions.save(current);
        sessions.save(RefreshSession.start(
                replacementId,
                user.id(),
                hashToken(replacementToken),
                current.familyId(),
                now,
                expiresAt));
        return RefreshOutcome.success(issueTokens(user, replacementToken, expiresAt));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        Instant now = clock.instant();
        sessions.findByTokenHashForUpdate(hashToken(rawRefreshToken)).ifPresent(session -> {
            session.revoke(now);
            sessions.save(session);
            sessions.revokeFamily(session.familyId(), now);
        });
    }

    @Transactional(readOnly = true)
    public UserSummary me(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        return UserSummary.from(user);
    }

    @Transactional
    public boolean createBootstrapAdmin(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return false;
        if (users.adminExists() || users.existsByEmail(normalizeEmail(email))) return false;
        users.save(normalizeEmail(email), passwordHasher.hash(password), true, Set.of(Role.ADMIN));
        return true;
    }

    private AuthTokens startSession(User user, UUID familyId) {
        Instant now = clock.instant();
        String token = newRefreshToken();
        Instant expiresAt = now.plus(refreshProperties.getTokenTtl());
        sessions.save(RefreshSession.start(UUID.randomUUID(), user.id(), hashToken(token), familyId, now, expiresAt));
        return issueTokens(user, token, expiresAt);
    }

    private AuthTokens issueTokens(User user, String rawRefreshToken, Instant refreshExpiresAt) {
        AccessTokenIssuer.IssuedAccessToken issued = accessTokenIssuer.issue(
                user, clock.instant(), jwtProperties.getAccessTokenTtl());
        return new AuthTokens(
                new AccessResponse(issued.value(), "Bearer", issued.expiresInSeconds(), UserSummary.from(user)),
                rawRefreshToken,
                refreshExpiresAt);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    public record AuthTokens(AccessResponse response, String refreshToken, Instant refreshExpiresAt) { }
    public record RefreshOutcome(boolean valid, AuthTokens tokens) {
        static RefreshOutcome success(AuthTokens tokens) { return new RefreshOutcome(true, tokens); }
        static RefreshOutcome failed() { return new RefreshOutcome(false, null); }
    }
    public record AccessResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) { }
    public record UserSummary(UUID id, String email, Set<String> roles) {
        static UserSummary from(User user) {
            return new UserSummary(user.id(), user.email(), user.roles().stream().map(Role::name).collect(java.util.stream.Collectors.toUnmodifiableSet()));
        }
    }
}
