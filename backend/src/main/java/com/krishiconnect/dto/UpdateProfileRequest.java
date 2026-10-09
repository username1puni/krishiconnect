package com.krishiconnect.dto;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required") @Size(max = 100) String fullName,
        @Email(message = "Enter a valid email") @Size(max = 120) String email,
        @Size(max = 80) String village,
        @Size(max = 60) String district,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Pattern(regexp = "en|kn|hi", message = "Language must be en, kn or hi") String preferredLang,
        @Size(max = 10) List<@Pattern(regexp = "[A-Z_]{2,20}") String> crops) {
}
