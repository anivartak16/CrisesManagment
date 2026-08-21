package com.crisesmanagment.crisesmanagment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrisesManagmentApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrisesManagmentApplication.class, args);
	}

}