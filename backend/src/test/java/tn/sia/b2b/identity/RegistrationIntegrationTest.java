package tn.sia.b2b.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tn.sia.b2b.identity.repository.UserRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RegistrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sia_b2b_test")
            .withUsername("sia")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Redis désactivé pour ce test
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6380");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9999");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    UserRepository userRepository;

    @Test
    void inscription_valide_cree_compte_pending() {
        var req = Map.of(
            "companyName", "Garage Test",
            "taxId", "1234567/A/M/000",
            "contactName", "Test User",
            "email", "test@garage.tn",
            "phone", "+216 71 000 000",
            "password", "motdepasse123",
            "address", Map.of("ville", "Tunis")
        );

        ResponseEntity<Map> resp = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/auth/register", req, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsEntry("status", "PENDING");

        assertThat(userRepository.findByEmail("test@garage.tn"))
                .isPresent()
                .get()
                .satisfies(u -> {
                    assertThat(u.getStatus().name()).isEqualTo("PENDING");
                    assertThat(u.getCustomerSourceRef()).isNull();
                    assertThat(u.getPasswordHash()).doesNotContain("motdepasse123");
                });
    }

    @Test
    void inscription_email_doublon_retourne_erreur_uniforme() {
        var req = Map.of(
            "companyName", "Garage Doublon",
            "taxId", "9999999/A/M/000",
            "contactName", "Doublon User",
            "email", "doublon@garage.tn",
            "phone", "+216 71 111 111",
            "password", "motdepasse123",
            "address", Map.of()
        );

        rest.postForEntity("http://localhost:" + port + "/api/v1/auth/register", req, Map.class);
        ResponseEntity<Map> resp = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/auth/register", req, Map.class);

        // Doit retourner une erreur mais sans révéler que l'email est déjà pris
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
        // Le corps ne doit pas contenir de trace technique
        String body = resp.getBody() != null ? resp.getBody().toString() : "";
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("SQL");
    }

    @Test
    void inscription_champs_manquants_retourne_422() {
        var req = Map.of("email", "incomplet@test.tn");

        ResponseEntity<Map> resp = rest.postForEntity(
                "http://localhost:" + port + "/api/v1/auth/register", req, Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody()).containsKey("details");
    }
}
