package com.example.pipemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class PipemateApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipemateApplication.class, args);
    }

}
