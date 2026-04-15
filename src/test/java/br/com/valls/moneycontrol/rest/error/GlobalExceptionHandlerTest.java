package br.com.valls.moneycontrol.rest.error;

import br.com.valls.moneycontrol.domain.exception.CompanyNotFoundException;
import br.com.valls.moneycontrol.domain.exception.DuplicateSaleEntryException;
import br.com.valls.moneycontrol.infrastructure.rest.error.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Testa o GlobalExceptionHandler em modo standalone (sem contexto Spring).
 * WebTestClient.bindToController cria um servidor reativo isolado com o
 * controller e o advice — suficiente para validar todos os mapeamentos HTTP.
 */
class GlobalExceptionHandlerTest {

    // ── controller de apoio (exclusivo para este teste) ─────────────────────
    @RestController
    @RequestMapping("/test")
    static class TestController {

        record TestRequest(@NotBlank String name) {}

        @PostMapping("/validation")
        String validation(@RequestBody @Valid TestRequest req) {
            return req.name();
        }

        @GetMapping("/business")
        void business() {
            throw new DuplicateSaleEntryException("Entrada duplicada para a semana");
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new CompanyNotFoundException("Empresa não encontrada");
        }

        @GetMapping("/illegal")
        void illegal() {
            throw new IllegalArgumentException("Argumento inválido recebido");
        }
    }

    // ── WebTestClient standalone — sem Spring Boot, sem segurança ───────────
    private final WebTestClient webTestClient = WebTestClient
            .bindToController(new TestController())
            .controllerAdvice(new GlobalExceptionHandler())
            .build();

    // ── critério: campo obrigatório vazio → 400 com errors[0].field correto ─
    @Test
    void validation_whenBlankName_returns400WithFieldError() {
        webTestClient.post().uri("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[0].field").isEqualTo("name")
                .jsonPath("$.detail").isEqualTo("Validation failed");
    }

    // ── critério: BusinessException → 422 com ProblemDetail (não Map) ───────
    @Test
    void businessException_returns422WithProblemDetail() {
        webTestClient.get().uri("/test/business")
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Entrada duplicada para a semana")
                .jsonPath("$.status").isEqualTo(422);
    }

    // ── critério: EntityNotFoundException → 404 com ProblemDetail ───────────
    @Test
    void entityNotFoundException_returns404WithProblemDetail() {
        webTestClient.get().uri("/test/not-found")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Empresa não encontrada")
                .jsonPath("$.status").isEqualTo(404);
    }

    // ── IllegalArgumentException → 400 ──────────────────────────────────────
    @Test
    void illegalArgumentException_returns400() {
        webTestClient.get().uri("/test/illegal")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Argumento inválido recebido");
    }
}
