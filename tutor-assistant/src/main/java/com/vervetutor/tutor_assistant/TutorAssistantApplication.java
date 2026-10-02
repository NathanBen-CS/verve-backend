package com.vervetutor.tutor_assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@CrossOrigin(origins = "http://localhost:3000")
@SpringBootApplication
@EnableScheduling
public class TutorAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(TutorAssistantApplication.class, args);
	}

}
