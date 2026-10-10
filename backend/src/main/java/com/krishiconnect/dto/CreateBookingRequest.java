package com.krishiconnect.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One item is a normal booking; two or three items (tractor + driver) are booked all-or-nothing. */
public record CreateBookingRequest(
        @NotEmpty(message = "Add something to book") @Size(max = 3, message = "A bundle can have at most 3 items")
        List<@Valid BookingItemRequest> items,
        @NotNull(message = "Choose a start time") LocalDateTime startAt,
        @NotNull(message = "Choose an end time") LocalDateTime endAt,
        @Size(max = 500) String note) {
}
