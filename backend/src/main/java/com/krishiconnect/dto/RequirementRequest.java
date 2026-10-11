package com.krishiconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Skill;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequirementRequest(
        @NotBlank(message = "Title is required") @Size(max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull(message = "Say if you need equipment or a person") ResourceType needType,
        EquipmentCategory category,
        Skill skill,
        @Size(max = 30) String crop,
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("500") BigDecimal areaAcres,
        @NotNull(message = "Choose a start time") LocalDateTime startAt,
        @NotNull(message = "Choose an end time") LocalDateTime endAt,
        @NotBlank(message = "Village is required") @Size(max = 80) String village,
        @Size(max = 60) String district,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @DecimalMin(value = "0", inclusive = false) BigDecimal budget) {
}
