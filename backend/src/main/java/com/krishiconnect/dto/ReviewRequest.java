package com.krishiconnect.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(@NotNull Long bookingItemId,
                            @Min(value = 1, message = "Rating is 1 to 5 stars") @Max(value = 5, message = "Rating is 1 to 5 stars") int rating,
                            @Size(max = 1000) String comment) {
}
