package com.krishiconnect.dto;

import jakarta.validation.constraints.NotBlank;

/** identifier is the phone number or the email address. */
public record LoginRequest(@NotBlank(message = "Phone or email is required") String identifier,
                           @NotBlank(message = "Password is required") String password) {
}
