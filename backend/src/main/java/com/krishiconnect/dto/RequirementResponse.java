package com.krishiconnect.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.Requirement;
import com.krishiconnect.entity.RequirementStatus;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Skill;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RequirementResponse(Long id, String title, String description, ResourceType needType,
                                  EquipmentCategory category, Skill skill, String crop, BigDecimal areaAcres,
                                  LocalDateTime startAt, LocalDateTime endAt, String village, String district,
                                  double latitude, double longitude, BigDecimal budget, RequirementStatus status,
                                  Long seekerId, String seekerName, long quoteCount, Double distanceKm,
                                  Long acceptedQuoteId, Instant createdAt, List<QuoteResponse> quotes) {

    public static RequirementResponse from(Requirement r, long quoteCount, Double distanceKm, List<QuoteResponse> quotes) {
        return new RequirementResponse(r.getId(), r.getTitle(), r.getDescription(), r.getNeedType(), r.getCategory(),
                r.getSkill(), r.getCrop(), r.getAreaAcres(), r.getStartAt(), r.getEndAt(), r.getVillage(),
                r.getDistrict(), r.getLatitude(), r.getLongitude(), r.getBudget(), r.getStatus(), r.getSeeker().getId(),
                r.getSeeker().getFullName(), quoteCount, distanceKm, r.getAcceptedQuoteId(), r.getCreatedAt(), quotes);
    }
}
