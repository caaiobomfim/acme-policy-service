package com.acme.insurance.policy.infra.fraud;

import com.acme.insurance.policy.app.dto.fraud.FraudAnalysisResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "fraudClient",
        url = "${policy.fraud.base-url}",
        configuration = FraudFeignConfig.class
)
public interface FraudClient {
    @GetMapping("/fraud_analysis")
    FraudAnalysisResponse analyze(@RequestParam("orderId") String orderId,
                                  @RequestParam("customerId") String customerId);
}
