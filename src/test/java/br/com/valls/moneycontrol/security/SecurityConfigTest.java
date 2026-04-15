package br.com.valls.moneycontrol.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Valida as regras de autorização do SecurityConfig contra o servidor real
 * (sem Docker Compose — usa H2 em memória via src/test/resources/application.yaml).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityConfigTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    // ── critério: /actuator/health não exige autenticação ───────────────────
    @Test
    void actuatorHealth_doesNotRequireAuth() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().value(status ->
                        assert_(status != HttpStatus.UNAUTHORIZED.value(),
                                "/actuator/health não deve retornar 401, retornou: " + status));
    }

    // ── critério: /swagger-ui.html não exige autenticação ───────────────────
    @Test
    void swaggerUi_doesNotRequireAuth() {
        webTestClient.get().uri("/swagger-ui.html")
                .exchange()
                .expectStatus().value(status ->
                        assert_(status != HttpStatus.UNAUTHORIZED.value(),
                                "/swagger-ui.html não deve retornar 401, retornou: " + status));
    }

    // ── critério: /api-docs/** não exige autenticação ───────────────────────
    @Test
    void apiDocs_doesNotRequireAuth() {
        webTestClient.get().uri("/api-docs")
                .exchange()
                .expectStatus().value(status ->
                        assert_(status != HttpStatus.UNAUTHORIZED.value(),
                                "/api-docs não deve retornar 401, retornou: " + status));
    }

    // ── critério: rota protegida sem credenciais → 401 ──────────────────────
    @Test
    void protectedRoute_withoutAuth_returns401() {
        webTestClient.post().uri("/api/v1/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private static void assert_(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
