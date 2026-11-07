package br.com.coloniagaucha.backend.modules;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.coloniagaucha.backend.TestcontainersConfiguration;
import br.com.coloniagaucha.backend.modules.auth.application.port.AuthUserPort;
import br.com.coloniagaucha.backend.modules.auth.application.port.PasswordHasher;
import br.com.coloniagaucha.backend.modules.auth.application.AuthUseCases;
import br.com.coloniagaucha.backend.modules.auth.domain.Role;
import br.com.coloniagaucha.backend.modules.product.application.ProductUseCases;
import br.com.coloniagaucha.backend.modules.product.application.port.ProductPort;
import br.com.coloniagaucha.backend.modules.product.domain.Product;
import br.com.coloniagaucha.backend.modules.product.domain.UnitOfMeasure;
import br.com.coloniagaucha.backend.modules.stock.application.StockUseCases;
import br.com.coloniagaucha.backend.modules.stock.domain.MovementType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import jakarta.servlet.http.Cookie;
import br.com.coloniagaucha.backend.shared.domain.BusinessRuleException;
import org.springframework.dao.DataAccessException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ApiIntegrationTest {
    private static final String PASSWORD = "correct-horse-battery-staple-1";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthUserPort users;
    @Autowired PasswordHasher passwords;
    @Autowired AuthUseCases auth;
    @Autowired ProductUseCases productUseCases;
    @Autowired ProductPort productPort;
    @Autowired StockUseCases stockUseCases;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void adminCanManageProductsSuppliersAndStockWithPersistedBalances() throws Exception {
        Session admin = login(Role.ADMIN);

        String product = """
                {"name":"Arroz %s","description":"Tipo 1","category":"Grãos","unitOfMeasure":"g","price":12.50}
                """.formatted(UUID.randomUUID());
        MvcResult created = mvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON).content(product))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        UUID productId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(get("/api/v1/products").param("name", "arroz").param("active", "true")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        mvc.perform(patch("/api/v1/products/{id}", productId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Arroz longo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Arroz longo"));

        mvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fornecedor " + UUID.randomUUID() + "\",\"email\":\"compras@example.com\",\"phone\":\"+55 51 3333-0000\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/stock/movements")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"type\":\"ENTRY\",\"quantity\":\"5.000\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value(admin.userId().toString()));

        mvc.perform(get("/api/v1/stock/balances").param("productId", productId.toString())
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5.0))
                .andExpect(jsonPath("$.unitOfMeasure").value("g"));

        mvc.perform(post("/api/v1/stock/movements")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"type\":\"EXIT\",\"quantity\":\"6.000\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/stock/balances").param("productId", productId.toString())
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5.0));

        mvc.perform(patch("/api/v1/products/{id}/deactivate", productId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isNoContent());
        mvc.perform(patch("/api/v1/products/{id}/reactivate", productId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void allBusinessRoutesRequireBearerAndStaffCannotWriteProductCatalog() throws Exception {
        mvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
        Session staff = login(Role.STAFF);
        mvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + staff.accessToken()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + staff.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sem permissão\",\"unitOfMeasure\":\"un\",\"price\":\"2.00\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void refreshRotatesOpaqueCookieAndReuseRevokesItsFamily() throws Exception {
        Session admin = login(Role.ADMIN);
        assertNotNull(admin.refreshCookie());
        assertNotNull(admin.refreshCookie().getValue());

        Csrf csrf = csrf();
        MvcResult refresh = mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrf.cookie(), admin.refreshCookie())
                        .header(csrf.headerName(), csrf.headerValue()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(admin.refreshCookie().getValue()))))
                .andReturn();
        Cookie rotated = refresh.getResponse().getCookie("refresh_token");
        assertNotNull(rotated);
        assertNotEquals(admin.refreshCookie().getValue(), rotated.getValue());

        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrf.cookie(), admin.refreshCookie())
                        .header(csrf.headerName(), csrf.headerValue()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrf.cookie(), rotated)
                        .header(csrf.headerName(), csrf.headerValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshAndClearsCookie() throws Exception {
        Session admin = login(Role.ADMIN);
        Csrf csrf = csrf();
        MvcResult logout = mvc.perform(post("/api/v1/auth/logout")
                        .cookie(csrf.cookie(), admin.refreshCookie())
                        .header(csrf.headerName(), csrf.headerValue()))
                .andExpect(status().isNoContent())
                .andReturn();
        String cookieHeader = logout.getResponse().getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertTrue(cookieHeader.contains("Max-Age=0"));
        mvc.perform(post("/api/v1/auth/refresh")
                        .cookie(csrf.cookie(), admin.refreshCookie())
                        .header(csrf.headerName(), csrf.headerValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfAndCorsProtectCookieAuthenticationEndpoints() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"password\":\"bad\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void concurrentRefreshOfSameCookieAllowsOneRotationAndRevokesFamilyOnReplay() throws Exception {
        Session admin = login(Role.ADMIN);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return auth.refresh(admin.refreshCookie().getValue()); });
            var second = executor.submit(() -> { start.await(); return auth.refresh(admin.refreshCookie().getValue()); });
            start.countDown();
            AuthUseCases.RefreshOutcome one = first.get();
            AuthUseCases.RefreshOutcome two = second.get();
            org.junit.jupiter.api.Assertions.assertEquals(1, (one.valid() ? 1 : 0) + (two.valid() ? 1 : 0));
            AuthUseCases.RefreshOutcome winner = one.valid() ? one : two;
            org.junit.jupiter.api.Assertions.assertFalse(auth.refresh(winner.tokens().refreshToken()).valid());
        }
    }

    @Test
    void concurrentStockExitsCannotMakeBalanceNegativeAndHistoryCannotBeChanged() throws Exception {
        Session admin = login(Role.ADMIN);
        String name = "Produto concorrente " + UUID.randomUUID();
        Product product = productUseCases.create(new ProductUseCases.CreateProduct(
                name, null, null, UnitOfMeasure.UNIDADE, new BigDecimal("1.00")));
        var entry = stockUseCases.create(new StockUseCases.CreateMovement(
                product.id(), null, MovementType.ENTRY, new BigDecimal("1.000"), null, admin.userId()));

        assertThrows(BusinessRuleException.class, () -> productUseCases.update(product.id(),
                new ProductUseCases.UpdateProduct(null, null, null, UnitOfMeasure.MILILITRO, null)));

        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptExit(start, product.id(), admin.userId()));
            var second = executor.submit(() -> attemptExit(start, product.id(), admin.userId()));
            start.countDown();
            boolean firstSucceeded = first.get();
            boolean secondSucceeded = second.get();
            org.junit.jupiter.api.Assertions.assertNotEquals(firstSucceeded, secondSucceeded);
        }
        org.junit.jupiter.api.Assertions.assertEquals(BigDecimal.ZERO.setScale(3), stockUseCases.balance(product.id()).quantity());
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "UPDATE stock_movements SET reason = 'alterado' WHERE id = ?", entry.id()));
    }

    @Test
    void concurrentCreatesStillRespectDatabaseNameUniqueness() throws Exception {
        String sameName = "Nome único concorrente " + UUID.randomUUID();
        var command = new ProductUseCases.CreateProduct(sameName, null, null, UnitOfMeasure.UNIDADE, new BigDecimal("1.00"));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptCreate(start, command));
            var second = executor.submit(() -> attemptCreate(start, command));
            start.countDown();
            org.junit.jupiter.api.Assertions.assertNotEquals(first.get(), second.get());
        }
        org.junit.jupiter.api.Assertions.assertTrue(productPort.findByNameKey(Product.nameKey(sameName)).isPresent());
    }

    private boolean attemptExit(CountDownLatch start, UUID productId, UUID userId) throws Exception {
        start.await();
        try {
            stockUseCases.create(new StockUseCases.CreateMovement(productId, null, MovementType.EXIT,
                    new BigDecimal("1.000"), null, userId));
            return true;
        } catch (BusinessRuleException exception) {
            return false;
        }
    }

    private boolean attemptCreate(CountDownLatch start, ProductUseCases.CreateProduct command) throws Exception {
        start.await();
        try {
            productUseCases.create(command);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Session login(Role role) throws Exception {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        var user = users.save(email, passwords.hash(PASSWORD), true, Set.of(role));
        Csrf csrf = csrf();
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf.cookie())
                        .header(csrf.headerName(), csrf.headerValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value(user.id().toString()))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie refresh = result.getResponse().getCookie("refresh_token");
        assertNotNull(refresh);
        String header = result.getResponse().getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertTrue(header.contains("HttpOnly"));
        org.junit.jupiter.api.Assertions.assertTrue(header.contains("SameSite=Lax"));
        org.junit.jupiter.api.Assertions.assertFalse(body.has("refreshToken"));
        return new Session(body.get("accessToken").asText(), refresh, user.id());
    }

    private Csrf csrf() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie, "endpoint CSRF precisa emitir cookie legível pelo frontend");
        return new Csrf(cookie, "X-XSRF-TOKEN", cookie.getValue());
    }

    private record LoginRequest(String email, String password) { }
    private record Session(String accessToken, Cookie refreshCookie, UUID userId) { }
    private record Csrf(Cookie cookie, String headerName, String headerValue) { }
}
