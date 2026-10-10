package com.krishiconnect.dto;

import com.krishiconnect.entity.EquipmentStatus;

import jakarta.validation.constraints.NotNull;

public record EquipmentStatusRequest(@NotNull EquipmentStatus status) {
}
