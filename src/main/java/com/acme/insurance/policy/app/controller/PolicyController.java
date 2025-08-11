package com.acme.insurance.policy.app.controller;

import com.acme.insurance.policy.domain.ports.in.*;
import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/policies")
public class PolicyController {

    private static final Logger log = LoggerFactory.getLogger(PolicyController.class);

    private final CreatePolicyUseCase createPolicyUseCase;
    private final GetPolicyByIdQuery getPolicyByIdQuery;
    private final ListPoliciesByCustomerQuery listPoliciesByCustomerQuery;
    private final CancelPolicyUseCase cancelPolicyUseCase;

    public PolicyController(CreatePolicyUseCase createPolicyUseCase,
                            GetPolicyByIdQuery getPolicyByIdQuery,
                            ListPoliciesByCustomerQuery listPoliciesByCustomerQuery,
                            CancelPolicyUseCase cancelPolicyUseCase) {
        this.createPolicyUseCase = createPolicyUseCase;
        this.getPolicyByIdQuery = getPolicyByIdQuery;
        this.listPoliciesByCustomerQuery = listPoliciesByCustomerQuery;
        this.cancelPolicyUseCase = cancelPolicyUseCase;
    }

    @PostMapping
    public ResponseEntity<PolicyResponseDto> create(@Valid @RequestBody PolicyRequestDto requestDto) {
        log.info("[POST] Criando nova policy para customerId={}", requestDto.customerId());
        PolicyResponseDto created = createPolicyUseCase.execute(requestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        log.info("[POST] Policy criada com sucesso - policyId={}", created.id());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponseDto> get(@PathVariable UUID id) {
        log.info("[GET] Buscando policy por id={}", id);
        PolicyResponseDto policy = getPolicyByIdQuery.execute(id);
        log.info("[GET] Policy encontrada - id={} status={}", policy.id(), policy.status());
        return ResponseEntity.ok(policy);
    }

    @GetMapping
    public ResponseEntity<List<PolicyResponseDto>> list(@RequestParam UUID customerId) {
        log.info("[GET] Buscando policies para customerId={}", customerId);
        List<PolicyResponseDto> policies = listPoliciesByCustomerQuery.execute(customerId);
        log.info("[GET] Encontradas {} policies para customerId={}", policies.size(), customerId);
        return ResponseEntity.ok(policies);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PolicyResponseDto> cancel(@PathVariable UUID id) {
        log.info("[HTTP] Cancelamento de policy solicitado - id={}", id);
        PolicyResponseDto cancelled = cancelPolicyUseCase.execute(id);
        log.info("[HTTP] Policy cancelada com sucesso - id={} status={} finishedAt={}",
                cancelled.id(), cancelled.status(), cancelled.finishedAt());
        return ResponseEntity.noContent().build();
    }
}
