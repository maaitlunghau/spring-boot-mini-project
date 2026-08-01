package com.maaitlunghau.spring_boot_mini_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Spring Boot Mini Project API")
                .description("Authentication and user management API")
                .version("v1"))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                    .name(BEARER_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
