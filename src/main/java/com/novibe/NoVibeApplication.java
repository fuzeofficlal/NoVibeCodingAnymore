package com.novibe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NoVibeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoVibeApplication.class, args);
    }
}
