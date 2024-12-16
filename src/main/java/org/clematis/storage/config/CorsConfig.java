package org.clematis.storage.config;

import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * @author Ruslan Lagay
 */
@Component
public class CorsConfig implements RepositoryRestConfigurer {

    public static final String ALL_REGEXP = "/**";
    public static final String ORIGINS = "*";

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {

        cors.addMapping(ALL_REGEXP)
            .allowedOrigins(ORIGINS)
            .allowedMethods(HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name(),
                HttpMethod.HEAD.name())
            .allowCredentials(false)
            .maxAge(3600);
    }
}

