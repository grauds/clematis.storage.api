package org.clematis.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Default Spring application
 */
@SpringBootApplication
@SuppressWarnings({"PMD", "checkstyle:hideutilityclassconstructor"})
@SuppressFBWarnings("EI_EXPOSE_REP")
public class Application {

    public static final String ALL_REGEXP = "/**";
    public static final String ORIGINS = "*";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping(ALL_REGEXP).allowedOrigins(ORIGINS);
            }
        };
    }
}
