package com.acme.insurance.policy.app.mapper;

import com.acme.insurance.policy.app.dto.PolicyRequestDto;
import com.acme.insurance.policy.app.dto.PolicyResponseDto;
import com.acme.insurance.policy.domain.model.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(componentModel = "spring", imports = {OffsetDateTime.class})
public interface ApiPolicyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "RECEIVED")
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "finishedAt", ignore = true)
    @Mapping(target = "history", ignore = true)
    Policy toDomain(PolicyRequestDto dto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "salesChannel", source = "salesChannel")
    @Mapping(target = "paymentMethod", source = "paymentMethod")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "finishedAt", source = "finishedAt")
    @Mapping(target = "totalMonthlyPremiumAmount", source = "totalMonthlyPremiumAmount")
    @Mapping(target = "insuredAmount", source = "insuredAmount")
    @Mapping(target = "coverages", source = "coverages")
    @Mapping(target = "assistances", source = "assistances")
    @Mapping(target = "history", expression = "java(mapHistory(domain.history()))")
    PolicyResponseDto toResponse(Policy domain);

    default List<PolicyResponseDto.StatusHistoryDto> mapHistory(List<Policy.StatusHistory> history) {
        if (history == null) return List.of();
        return history.stream()
                .map(h -> new PolicyResponseDto.StatusHistoryDto(h.status().name(), h.timestamp()))
                .toList();
    }
}
