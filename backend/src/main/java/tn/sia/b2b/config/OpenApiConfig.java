package tn.sia.b2b.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIA B2B — API de commande professionnelle")
                        .description("Plateforme de commande B2B pour SIA, distributeur de pièces automobiles. " +
                                "Les DTO marqués TBD-xx seront complétés après accord du client sur les structures YME.")
                        .version("0.1.0")
                        .contact(new Contact().name("SIA").email("dev@sia.tn")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Développement local")
                ));
    }
}
