package com.krishiconnect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LanguageRequest(@NotNull @Pattern(regexp = "en|kn|hi", message = "Language must be en, kn or hi") String lang) {
}
