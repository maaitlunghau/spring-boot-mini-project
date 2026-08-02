package com.maaitlunghau.spring_boot_mini_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringBootMiniProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootMiniProjectApplication.class, args);
	}

}
