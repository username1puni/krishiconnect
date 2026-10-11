package com.krishiconnect.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishiconnect.entity.Quote;
import com.krishiconnect.entity.QuoteStatus;
import com.krishiconnect.entity.Role;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuoteResponse(Long id, Long requirementId, String requirementTitle, Long providerId,
                            String providerName, Role providerRole, boolean providerVerified, String providerVillage,
                            Long equipmentId, String equipmentTitle, BigDecimal amount, String message,
                            QuoteStatus status, Long bookingId, Instant createdAt) {

    public static QuoteResponse from(Quote q) {
        var p = q.getProvider();
        var e = q.getEquipment();
        return new QuoteResponse(q.getId(), q.getRequirement().getId(), q.getRequirement().getTitle(), p.getId(),
                p.getFullName(), p.getRole(), p.isVerified(), p.getVillage(), e == null ? null : e.getId(),
                e == null ? null : e.getTitle(), q.getAmount(), q.getMessage(), q.getStatus(), q.getBookingId(),
                q.getCreatedAt());
    }
}
