package br.com.coloniagaucha.backend.modules.auth.adapter.in.web;

import br.com.coloniagaucha.backend.modules.auth.application.AuthUseCases;
import br.com.coloniagaucha.backend.shared.config.BootstrapAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapper implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);
    private final AuthUseCases auth;
    private final BootstrapAdminProperties properties;

    public AdminBootstrapper(AuthUseCases auth, BootstrapAdminProperties properties) {
        this.auth = auth;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.getEmail().isBlank() || properties.getPassword().isBlank()) {
            log.info("Bootstrap admin ignorado; configure BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD apenas durante o provisionamento inicial.");
            return;
        }
        boolean created = auth.createBootstrapAdmin(properties.getEmail(), properties.getPassword());
        log.info(created ? "Administrador inicial provisionado." : "Bootstrap admin ignorado porque já existe um administrador ou e-mail cadastrado.");
    }
}
