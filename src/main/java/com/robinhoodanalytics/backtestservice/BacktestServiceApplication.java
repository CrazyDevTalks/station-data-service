package com.robinhoodanalytics.backtestservice;
import javax.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.context.annotation.Configuration
@org.springframework.boot.autoconfigure.EnableAutoConfiguration
@SpringBootApplication
public class BacktestServiceApplication {

	@Autowired
	private ObjectMapper objectMapper;

	public static void main(String[] args) {
		SpringApplication.run(BacktestServiceApplication.class, args);
	}

	@PostConstruct
	public void setUp() {
		objectMapper.registerModule(new JavaTimeModule());
	}
}
