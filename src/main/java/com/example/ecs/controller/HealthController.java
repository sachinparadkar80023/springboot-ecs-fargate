package com.example.ecs.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Spring Boot on AWS ECS Fargate!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "running");
        return response;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Spring Boot ECS Fargate Example");
        response.put("version", "1.0.0");
        response.put("java-version", System.getProperty("java.version"));
        response.put("os", System.getProperty("os.name"));
        return response;
    }
}
