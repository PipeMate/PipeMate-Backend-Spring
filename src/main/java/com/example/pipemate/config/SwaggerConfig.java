package com.example.pipemate.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI githubAccessTokenOpenAPI() {
        Info info = new Info()
                .title("PipeMate Server API")
                .description("GitHub 연동을 포함한 PipeMate 서버 API 명세서")
                .version("1.0.0");

        String githubSchemeName = "GitHub Access Token";

        // SecurityRequirement 설정 (각 API에 Authorization 적용)
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(githubSchemeName);

        // Components 설정 (Bearer 토큰 형식)
        Components components = new Components()
                .addSecuritySchemes(githubSchemeName,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("GitHubToken")); // 설명용 (필수 아님)

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}