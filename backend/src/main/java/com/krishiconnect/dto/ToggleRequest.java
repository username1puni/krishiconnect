package com.krishiconnect.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleRequest(@NotNull Boolean value) {
}
