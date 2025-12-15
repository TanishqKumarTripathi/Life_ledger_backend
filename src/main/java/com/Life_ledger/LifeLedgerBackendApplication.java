package com.Life_ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LifeLedgerBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(LifeLedgerBackendApplication.class, args);
	}

}
