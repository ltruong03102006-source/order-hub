package com.orderhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class OrderhubApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderhubApiApplication.class, args);
	}

}
