package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class SpringbootEcsFargateApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootEcsFargateApplication.class, args);
	}
	
	@GetMapping("/get")
	public String getData() {
		return "discussion about springboot deployment using  aws ecs and fargate";
	}

}
