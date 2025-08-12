package com.acme.insurance.policy.app.error;

import com.acme.insurance.policy.domain.model.PolicyStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.util.UUID;

public class PolicyCancelConflictException extends ErrorResponseException {

    public PolicyCancelConflictException(UUID policyId, String currentStatus) {
        super(HttpStatus.CONFLICT, problem(policyId, currentStatus), null);
    }

    public PolicyCancelConflictException(UUID policyId, PolicyStatus currentStatus) {
        this(policyId, currentStatus != null ? currentStatus.name() : null);
    }

    private static ProblemDetail problem(UUID id, String currentStatus) {
        var pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "It is not possible to cancel a policy with a final status" +
                        (currentStatus != null ? (": " + currentStatus) : "")
        );
        pd.setTitle("Policy cancel conflict");
        pd.setType(URI.create("https://api.acme.com/errors/policy-cancel-conflict"));
        pd.setProperty("policyId", id != null ? id.toString() : null);
        pd.setProperty("currentStatus", currentStatus);
        return pd;
    }
}