package org.clematis.storage.config;

import static org.springdoc.core.utils.Constants.ALL_PATTERN;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 *
 * @author Anton Troshin
 */
@Configuration
@ComponentScan(
    basePackages = {"org.springdoc"}
)
public class OpenAPIConfig {

    private final BuildProperties buildProperties;

    public OpenAPIConfig(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(getInfo());
    }

    public Info getInfo() {
        return new Info().title(" API")
                .summary("Hateoas Restful API for a file storage")
                .description("")
                .version(buildProperties.getVersion())
                .description(buildProperties.getName());
    }

    @Bean
    public GroupedOpenApi filesApi() {
        return GroupedOpenApi.builder()
                .group("Files")
                .pathsToMatch("/api/files")
                .build();
    }

    @Bean
    public GroupedOpenApi dbApi() {
        return GroupedOpenApi.builder()
            .group("Database")
            .pathsToMatch("/api/db")
            .build();
    }

    @Bean
    SwaggerUiConfigProperties swaggerUiConfig() {
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        config.setShowCommonExtensions(true);
        return config;
    }

    @Bean
    public GroupedOpenApi actuatorApi(WebEndpointProperties endpointProperties) {
        return GroupedOpenApi.builder()
            .group("Actuator")
            .pathsToMatch(endpointProperties.getBasePath() + ALL_PATTERN)
            .addOpenApiCustomizer(openApi -> openApi.info(
                    new Info()
                        .title("Storage Actuator API")
                        .version(buildProperties.getVersion())
                )
            )
            .build();
    }

}
