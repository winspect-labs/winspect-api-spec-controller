package com.winspect.apispec.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiSpecControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiSpecControllerApplication.class, args);
    }
}
