package com.nectar.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NectarWorkflowPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(NectarWorkflowPlatformApplication.class, args);
	}

}
