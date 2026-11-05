package com.plainledger;

import org.springframework.boot.SpringApplication;

/**
 * Runs the application locally against a throwaway PostgreSQL container: {@code ./mvnw
 * spring-boot:test-run}. Needs Docker, nothing else.
 */
public class TestPlainledgerApplication {

    public static void main(String[] args) {
        SpringApplication.from(PlainledgerApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
