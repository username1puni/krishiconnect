package com.krishiconnect.dto;

import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.Skill;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required") @Size(max = 100) String fullName,
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a 10 digit mobile number") String phone,
        @Email(message = "Enter a valid email") @Size(max = 120) String email,
        @NotBlank @Size(min = 6, max = 72, message = "Password must be at least 6 characters") String password,
        @NotNull(message = "Choose a role") Role role,
        @Size(max = 80) String village,
        @Size(max = 60) String district,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Pattern(regexp = "en|kn|hi", message = "Language must be en, kn or hi") String preferredLang,
        Skill skill) {
}
