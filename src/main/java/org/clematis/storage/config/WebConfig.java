package org.clematis.storage.config;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@Configuration
//@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    public static final String ALL_REGEXP = "/**";
    public static final String ORIGINS = "*";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(ALL_REGEXP).allowedOrigins(ORIGINS);
    }
}