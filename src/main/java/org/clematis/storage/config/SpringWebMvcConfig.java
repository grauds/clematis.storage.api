package org.clematis.storage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class SpringWebMvcConfig implements WebMvcConfigurer {

    public static final String ALL_REGEXP = "/**";
    public static final String ORIGINS = "*";

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping(ALL_REGEXP)
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