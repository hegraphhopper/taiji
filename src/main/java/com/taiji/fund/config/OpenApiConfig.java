package com.taiji.fund.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI fundOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("自投研发项目重复投入治理平台 API")
                        .version("1.0.0")
                        .description("平台底座与最小鉴权接口（A1）"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }

    @Bean
    public GroupedOpenApi authOpenApi() {
        return GroupedOpenApi.builder()
                .group("01-认证")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi systemOpenApi() {
        return GroupedOpenApi.builder()
                .group("02-系统基础数据")
                .pathsToMatch("/api/v1/groups/**", "/api/v1/tech-directions/**", "/api/v1/dicts/**", "/api/v1/health/**")
                .build();
    }
}
