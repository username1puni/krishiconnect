package com.krishiconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI krishiConnectOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("KrishiConnect API")
                        .version("1.0.0")
                        .description("Rent farm equipment, hire skilled workers and post requirements. Log in with "
                                + "/api/auth/login and paste the token into Authorize to try the protected endpoints.")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
