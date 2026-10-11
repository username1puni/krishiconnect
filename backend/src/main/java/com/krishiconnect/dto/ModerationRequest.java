package com.krishiconnect.dto;

import com.krishiconnect.entity.ModerationStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerationRequest(@NotNull ModerationStatus status, @Size(max = 255) String note) {
}
