package br.com.coloniagaucha.backend.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.jwt")
public class JwtProperties {
    private String issuer = "https://api.coloniagaucha.local";
    private String audience = "colonia-gaucha-api";
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private String publicKeyPem = "";
    private String privateKeyPem = "";
    private boolean ephemeralDevKey;

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public Duration getAccessTokenTtl() { return accessTokenTtl; }
    public void setAccessTokenTtl(Duration accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
    public String getPublicKeyPem() { return publicKeyPem; }
    public void setPublicKeyPem(String publicKeyPem) { this.publicKeyPem = publicKeyPem; }
    public String getPrivateKeyPem() { return privateKeyPem; }
    public void setPrivateKeyPem(String privateKeyPem) { this.privateKeyPem = privateKeyPem; }
    public boolean isEphemeralDevKey() { return ephemeralDevKey; }
    public void setEphemeralDevKey(boolean ephemeralDevKey) { this.ephemeralDevKey = ephemeralDevKey; }
}
