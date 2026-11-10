package com.dtech.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class AdminApplication {

	// Build marker: validates the UAT CI/CD pipeline without changing runtime behavior.
	public static void main(String[] args) {
		SpringApplication.run(AdminApplication.class, args);
	}

}
