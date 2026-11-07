package br.com.coloniagaucha.backend.modules.auth.adapter.out.security;

import br.com.coloniagaucha.backend.modules.auth.application.port.AccessTokenIssuer;
import br.com.coloniagaucha.backend.modules.auth.domain.User;
import br.com.coloniagaucha.backend.shared.config.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public JwtAccessTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public IssuedAccessToken issue(User user, Instant now, Duration lifetime) {
        Instant expiresAt = now.plus(lifetime);
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").keyId("colonia-access-v1").build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.id().toString())
                .audience(List.of(properties.getAudience()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("roles", user.roles().stream().map(Enum::name).sorted().toList())
                .claim("token_use", "access")
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, lifetime.toSeconds());
    }
}
