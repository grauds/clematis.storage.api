package org.clematis.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Default Spring application
 */
@SpringBootApplication
@ComponentScan({"org.clematis.storage", "org.clematis.logging"})
@SuppressWarnings({"PMD", "checkstyle:hideutilityclassconstructor"})
@SuppressFBWarnings("EI_EXPOSE_REP")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
