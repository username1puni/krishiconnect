package com.krishiconnect.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.BookingResponse;
import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.entity.BookingItemStatus;
import com.krishiconnect.security.AuthUser;
import com.krishiconnect.service.BookingService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** Booking requests received by equipment owners and specialists. */
@Tag(name = "Bookings")
@RestController
@RequestMapping("/api/provider/bookings")
@PreAuthorize("hasAnyRole('PROVIDER', 'SPECIALIST')")
@RequiredArgsConstructor
public class ProviderBookingController {

    private final BookingService bookingService;

    @GetMapping
    public PageResponse<BookingResponse> incoming(@AuthenticationPrincipal AuthUser me,
                                                  @RequestParam(required = false) BookingItemStatus status,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return bookingService.incoming(me.getId(), status, page, size);
    }

    @PostMapping("/items/{itemId}/accept")
    public BookingResponse accept(@AuthenticationPrincipal AuthUser me, @PathVariable Long itemId) {
        return bookingService.acceptItem(me.getId(), itemId);
    }

    @PostMapping("/items/{itemId}/reject")
    public BookingResponse reject(@AuthenticationPrincipal AuthUser me, @PathVariable Long itemId) {
        return bookingService.rejectItem(me.getId(), itemId);
    }

    @PostMapping("/items/{itemId}/complete")
    public BookingResponse complete(@AuthenticationPrincipal AuthUser me, @PathVariable Long itemId) {
        return bookingService.completeItem(me.getId(), itemId);
    }
}
