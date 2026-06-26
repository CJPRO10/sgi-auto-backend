package com.sgi.auto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutoApplication {
	public static void main(String[] args) {
		SpringApplication.run(AutoApplication.class, args);
	}
}
