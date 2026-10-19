package com.example.mocktester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
public class MockTesterApplication {

    public static void main(String[] args) {
        // SQLite does not create parent directories, so make sure the folder exists.
        String db = System.getProperty("APP_DB_PATH", System.getenv().getOrDefault("APP_DB_PATH", "./data/mocktester.db"));
        try {
            Path parent = Path.of(db).toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        SpringApplication.run(MockTesterApplication.class, args);
    }
}
