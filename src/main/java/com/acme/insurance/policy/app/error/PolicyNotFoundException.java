package com.acme.insurance.policy.app.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.util.UUID;

public class PolicyNotFoundException extends ErrorResponseException {

    public PolicyNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, problem(id), null);
    }

    private static ProblemDetail problem(UUID id) {
        var pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Policy not found");
        pd.setTitle("Not Found");
        pd.setType(URI.create("https://api.acme.com/errors/policy-not-found"));
        pd.setProperty("policyId", id.toString());
        return pd;
    }
}
