package br.com.coloniagaucha.backend.modules.auth.adapter.in.web;

import br.com.coloniagaucha.backend.modules.auth.application.AuthUseCases;
import br.com.coloniagaucha.backend.modules.auth.domain.InvalidAuthException;
import br.com.coloniagaucha.backend.shared.config.RefreshProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthUseCases auth;
    private final RefreshProperties cookieProperties;
    private final Clock clock;

    public AuthController(AuthUseCases auth, RefreshProperties cookieProperties, Clock clock) {
        this.auth = auth;
        this.cookieProperties = cookieProperties;
        this.clock = clock;
    }

    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return new CsrfResponse("XSRF-TOKEN", csrfToken.getHeaderName());
    }

    @PostMapping("/login")
    AuthUseCases.AccessResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthUseCases.AuthTokens tokens = auth.login(request.email(), request.password());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), tokens.refreshExpiresAt()).toString());
        return tokens.response();
    }

    @PostMapping("/refresh")
    AuthUseCases.AccessResponse refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String rawToken = cookieValue(request, cookieProperties.getCookieName());
        AuthUseCases.RefreshOutcome outcome = auth.refresh(rawToken);
        if (!outcome.valid()) {
            response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString());
            throw new InvalidAuthException();
        }
        AuthUseCases.AuthTokens tokens = outcome.tokens();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), tokens.refreshExpiresAt()).toString());
        return tokens.response();
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            HttpServletRequest request) {
        String rawToken = cookieValue(request, cookieProperties.getCookieName());
        auth.logout(rawToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString()).build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    AuthUseCases.UserSummary me(@AuthenticationPrincipal Jwt jwt) {
        return auth.me(UUID.fromString(jwt.getSubject()));
    }

    private ResponseCookie refreshCookie(String token, java.time.Instant expiresAt) {
        Duration maxAge = Duration.between(clock.instant(), expiresAt);
        return ResponseCookie.from(cookieProperties.getCookieName(), token)
                .httpOnly(true)
                .secure(cookieProperties.isCookieSecure())
                .sameSite(cookieProperties.getCookieSameSite())
                .path(cookieProperties.getCookiePath())
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(cookieProperties.getCookieName(), "")
                .httpOnly(true)
                .secure(cookieProperties.isCookieSecure())
                .sameSite(cookieProperties.getCookieSameSite())
                .path(cookieProperties.getCookiePath())
                .maxAge(Duration.ZERO)
                .build();
    }

    private static String cookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return java.util.Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password) { }

    public record CsrfResponse(String cookieName, String headerName) { }
}
