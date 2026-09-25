package com.eventsphere.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui.html. Click "Authorize" and paste the token from POST /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventSphereOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventSphere API")
                        .version("1.0.0")
                        .description("AI powered event management: events, sessions, speakers, smart registration "
                                + "with waitlist, QR tickets & check-in, feedback and analytics.")
                        .contact(new Contact().name("EventSphere")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
