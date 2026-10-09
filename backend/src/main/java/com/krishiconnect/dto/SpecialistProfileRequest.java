package com.krishiconnect.dto;

import java.math.BigDecimal;

import com.krishiconnect.entity.Skill;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpecialistProfileRequest(
        @NotNull(message = "Choose your main skill") Skill skill,
        @Size(max = 500) String bio,
        @Min(0) @Max(60) int experienceYears,
        @DecimalMin(value = "0", inclusive = false, message = "Rate must be more than 0") BigDecimal ratePerHour,
        @DecimalMin(value = "0", inclusive = false, message = "Rate must be more than 0") BigDecimal ratePerDay,
        @Size(max = 100) String languages,
        boolean available) {
}
