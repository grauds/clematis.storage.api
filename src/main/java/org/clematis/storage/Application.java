package org.clematis.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Default Spring application
 */
@SpringBootApplication
@SuppressWarnings({"PMD", "checkstyle:hideutilityclassconstructor"})
@SuppressFBWarnings("EI_EXPOSE_REP")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
