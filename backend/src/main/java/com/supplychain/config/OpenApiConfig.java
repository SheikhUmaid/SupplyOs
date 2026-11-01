package com.supplychain.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.ArrayList;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI supplyChainOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SupplyChain OS API")
                .version("v1")
                .description("Log in through POST /api/auth/login. The session is an HttpOnly cookie, "
                        + "which this page then sends automatically."));
    }

    /**
     * The frontend's TypeScript types are generated from this document. Every property of a response is
     * always present, so it is marked required. Fields that can be null declare it with @Schema(types).
     */
    @Bean
    OpenApiCustomizer responseFieldsAreRequired() {
        return openApi -> openApi.getComponents().getSchemas().forEach((name, schema) -> {
            if (name.contains("Response") && schema.getProperties() != null) {
                schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
            }
        });
    }
}
