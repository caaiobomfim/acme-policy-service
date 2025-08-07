package com.acme.insurance.policy_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PolicyServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
