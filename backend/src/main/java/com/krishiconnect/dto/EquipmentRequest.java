package com.krishiconnect.dto;

import java.math.BigDecimal;

import com.krishiconnect.entity.EquipmentCategory;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipmentRequest(
        @NotBlank(message = "Title is required") @Size(max = 120) String title,
        @NotNull(message = "Choose a category") EquipmentCategory category,
        @Size(max = 60) String brand,
        @Size(max = 60) String model,
        @Min(1980) @Max(2100) Integer yearOfMake,
        @Min(1) @Max(1000) Integer horsePower,
        @Size(max = 1000) String description,
        @DecimalMin(value = "0", inclusive = false) BigDecimal pricePerHour,
        @DecimalMin(value = "0", inclusive = false) BigDecimal pricePerDay,
        @DecimalMin(value = "0", inclusive = false) BigDecimal pricePerAcre,
        boolean withOperator,
        @NotBlank(message = "Village is required") @Size(max = 80) String village,
        @Size(max = 60) String district,
        @NotNull(message = "Pick the location on the map") @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull(message = "Pick the location on the map") @DecimalMin("-180") @DecimalMax("180") Double longitude) {
}
