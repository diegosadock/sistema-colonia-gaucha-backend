package br.com.coloniagaucha.backend.shared.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.cors")
public class CorsProperties {
    private String allowedOrigins = "http://localhost:3000";

    public List<String> getAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
}
