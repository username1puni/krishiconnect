package com.krishiconnect.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.BookingResponse;
import com.krishiconnect.dto.CreateBookingRequest;
import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.entity.BookingStatus;
import com.krishiconnect.security.AuthUser;
import com.krishiconnect.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "Bookings")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Book equipment, a specialist, or both together (all-or-nothing). "
            + "Answers 409 if any part is already booked for an overlapping time.")
    @PostMapping
    @PreAuthorize("!hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal AuthUser me,
                                                  @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse booking = bookingService.create(me.getId(), request);
        return ResponseEntity.created(URI.create("/api/bookings/" + booking.id())).body(booking);
    }

    @GetMapping
    public PageResponse<BookingResponse> mine(@AuthenticationPrincipal AuthUser me,
                                              @RequestParam(required = false) BookingStatus status,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return bookingService.mine(me.getId(), status, page, size);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return bookingService.get(id, me);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return bookingService.cancel(me.getId(), id);
    }
}
