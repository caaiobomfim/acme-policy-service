package com.acme.insurance.policy.app.controller;

import com.acme.insurance.policy.domain.ports.in.PolicyService;
import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/policies")
public class PolicyController {

    private static final Logger log = LoggerFactory.getLogger(PolicyController.class);

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponseDto> createPolicy(@Valid @RequestBody PolicyRequestDto requestDto) {
        log.info("[POST] Criando nova policy para customerId={}", requestDto.customerId());
        PolicyResponseDto created = policyService.createPolicy(requestDto);
        log.info("[POST] Policy criada com sucesso - policyId={}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponseDto> getPolicyById(@PathVariable UUID id) {
        log.info("[GET] Buscando policy por id={}", id);
        PolicyResponseDto policy = policyService.getPolicyById(id);
        log.info("[GET] Policy encontrada - id={} status={}", policy.id(), policy.status());
        return ResponseEntity.ok(policy);
    }

    @GetMapping
    public ResponseEntity<List<PolicyResponseDto>> getPoliciesByCustomerId(@RequestParam UUID customerId) {
        log.info("[GET] Buscando policies para customerId={}", customerId);
        List<PolicyResponseDto> policies = policyService.getPoliciesByCustomerId(customerId);
        log.info("[GET] Encontradas {} policies para customerId={}", policies.size(), customerId);
        return ResponseEntity.ok(policies);
    }
}
