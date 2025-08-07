package com.acme.insurance.policy_service;

import org.springframework.boot.SpringApplication;

public class TestPolicyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(PolicyServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
