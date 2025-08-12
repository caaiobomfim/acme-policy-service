package com.acme.insurance.policy.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
@ActiveProfiles("test")
public abstract class HttpIntegrationBase {

    protected static WireMockServer WIREMOCK;

    @BeforeAll
    static void startWireMock() {
        WIREMOCK = new WireMockServer(options().dynamicPort());
        WIREMOCK.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (WIREMOCK != null) WIREMOCK.stop();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("policy.fraud.base-url", () -> "http://localhost:" + WIREMOCK.port());
    }
}
