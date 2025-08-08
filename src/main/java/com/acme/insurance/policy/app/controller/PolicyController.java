package com.acme.insurance.policy.app.controller;

import com.acme.insurance.policy.domain.ports.in.PolicyService;
import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponseDto> createPolicy(@Valid @RequestBody PolicyRequestDto requestDto) {
        PolicyResponseDto created = policyService.createPolicy(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponseDto> getPolicyById(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @GetMapping
    public ResponseEntity<List<PolicyResponseDto>> getPoliciesByCustomerId(@RequestParam UUID customerId) {
        return ResponseEntity.ok(policyService.getPoliciesByCustomerId(customerId));
    }
}
