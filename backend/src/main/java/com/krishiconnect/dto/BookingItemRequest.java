package com.krishiconnect.dto;

import java.math.BigDecimal;

import com.krishiconnect.entity.PriceUnit;
import com.krishiconnect.entity.ResourceType;

import jakarta.validation.constraints.NotNull;

public record BookingItemRequest(@NotNull ResourceType type, @NotNull Long resourceId,
                                 @NotNull(message = "Choose how to pay: per hour, day or acre") PriceUnit priceUnit,
                                 BigDecimal acres) {
}
