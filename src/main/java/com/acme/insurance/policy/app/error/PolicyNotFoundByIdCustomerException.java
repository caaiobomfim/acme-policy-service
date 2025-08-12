package com.acme.insurance.policy.app.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.util.UUID;

public class PolicyNotFoundByIdCustomerException extends ErrorResponseException {

    public PolicyNotFoundByIdCustomerException(UUID customerId) {
        super(HttpStatus.NOT_FOUND, problem(customerId), null);
    }

    private static ProblemDetail problem(UUID customerId) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "No policy was found for this customer"
        );
        pd.setTitle("Not Found");
        pd.setType(URI.create("https://api.acme.com/errors/customer-policies-not-found"));
        pd.setProperty("customerId", customerId.toString());
        return pd;
    }
}