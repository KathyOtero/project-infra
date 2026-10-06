package com.caribexperience.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de OpenAPI/Swagger (Etapa 7).
 *
 * Expone la documentacion interactiva en /swagger-ui.html y el contrato en
 * /v3/api-docs. Se agrega el esquema de seguridad "bearerAuth" para poder
 * probar los endpoints protegidos desde la UI de Swagger pegando el JWT
 * obtenido en POST /api/auth/login (boton "Authorize").
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI caribeXperienceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CaribeXperience API")
                        .description("API REST para la plataforma de reserva de experiencias "
                                + "turisticas en la Costa Caribe colombiana. Proyecto academico "
                                + "de Ingenieria de Sistemas - UTB, grupo LOS PARRILLEROS.")
                        .version("v1")
                        .contact(new Contact().name("LOS PARRILLEROS")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
