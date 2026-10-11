package com.krishiconnect.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** equipmentId is required for equipment providers and left empty by specialists. */
public record QuoteRequest(Long equipmentId,
                           @NotNull(message = "Enter your price")
                           @DecimalMin(value = "1", message = "Enter your price")
                           @DecimalMax(value = "10000000") BigDecimal amount,
                           @Size(max = 500) String message) {
}
