package com.acme.insurance.policy.infra.dynamodb.mapper;

import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.infra.dynamodb.PolicyItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = PolicyMapConverters.class)
public interface PolicyItemMapper {

    // Domain -> Dynamo item
    @Mapping(target = "policyId", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "customerId", source = "customerId", qualifiedByName = "uuidToString")
    @Mapping(target = "productId", source = "productId", qualifiedByName = "uuidToString")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "odtToString")
    @Mapping(target = "finishedAt", source = "finishedAt", qualifiedByName = "odtToString")
    @Mapping(target = "totalMonthlyPremiumAmount", source = "totalMonthlyPremiumAmount", qualifiedByName = "bdToString")
    @Mapping(target = "insuredAmount", source = "insuredAmount", qualifiedByName = "bdToString")
    @Mapping(target = "coverages", source = "coverages", qualifiedByName = "toStringMap")
    @Mapping(target = "history", source = "history", qualifiedByName = "toHistoryMap")
    PolicyItem toItem(Policy domain);

    // Dynamo item -> Domain
    @Mapping(target = "id", source = "policyId", qualifiedByName = "stringToUuid")
    @Mapping(target = "customerId", source = "customerId", qualifiedByName = "stringToUuid")
    @Mapping(target = "productId", source = "productId", qualifiedByName = "stringToUuid")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "stringToOdt")
    @Mapping(target = "finishedAt", source = "finishedAt", qualifiedByName = "stringToOdt")
    @Mapping(target = "totalMonthlyPremiumAmount", source = "totalMonthlyPremiumAmount", qualifiedByName = "stringToBd")
    @Mapping(target = "insuredAmount", source = "insuredAmount", qualifiedByName = "stringToBd")
    @Mapping(target = "coverages", source = "coverages", qualifiedByName = "toBigDecimalMap")
    @Mapping(target = "history", source = "history", qualifiedByName = "fromHistoryMap")
    Policy toDomain(PolicyItem item);
}
