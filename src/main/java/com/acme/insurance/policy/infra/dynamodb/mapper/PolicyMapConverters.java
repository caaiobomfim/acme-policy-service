package com.acme.insurance.policy.infra.dynamodb.mapper;

import com.acme.insurance.policy.domain.model.Policy;
import com.acme.insurance.policy.domain.model.PolicyStatus;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PolicyMapConverters {

    private PolicyMapConverters() {}

    /* UUID <-> String */
    @Named("uuidToString")
    public static String uuidToString(UUID id) { return id != null ? id.toString() : null; }

    @Named("stringToUuid")
    public static UUID stringToUuid(String id) { return id != null ? UUID.fromString(id) : null; }

    /* OffsetDateTime <-> String (ISO-8601) */
    @Named("odtToString")
    public static String odtToString(OffsetDateTime odt) { return odt != null ? odt.toString() : null; }

    @Named("stringToOdt")
    public static OffsetDateTime stringToOdt(String s) { return s != null ? OffsetDateTime.parse(s) : null; }

    /* BigDecimal <-> String */
    @Named("bdToString")
    public static String bdToString(BigDecimal bd) { return bd != null ? bd.toPlainString() : null; }

    @Named("stringToBd")
    public static BigDecimal stringToBd(String s) { return s != null ? new BigDecimal(s) : null; }

    /* Map<String, BigDecimal> <-> Map<String, String> */
    @Named("toStringMap")
    public static Map<String, String> toStringMap(Map<String, BigDecimal> in) {
        if (in == null) return Map.of();
        return in.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toPlainString()));
    }

    @Named("toBigDecimalMap")
    public static Map<String, BigDecimal> toBigDecimalMap(Map<String, String> in) {
        if (in == null) return Map.of();
        return in.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new BigDecimal(e.getValue())));
    }

    /* History <-> Map representation */
    @Named("toHistoryMap")
    public static List<Map<String, String>> toHistoryMap(List<Policy.StatusHistory> hist) {
        if (hist == null || hist.isEmpty()) return List.of();
        return hist.stream()
                .map(h -> Map.of(
                        "status", h.status().name(),
                        "timestamp", h.timestamp().toString()
                ))
                .toList();
    }

    /* List<Map> (persistência) -> History (domain) */
    @Named("fromHistoryMap")
    public static List<Policy.StatusHistory> fromHistoryMap(List<Map<String, String>> maps) {
        if (maps == null || maps.isEmpty()) return List.of();
        return maps.stream()
                .map(m -> new Policy.StatusHistory(
                        PolicyStatus.valueOf(m.get("status")),
                        OffsetDateTime.parse(m.get("timestamp"))
                ))
                .toList();
    }
}
