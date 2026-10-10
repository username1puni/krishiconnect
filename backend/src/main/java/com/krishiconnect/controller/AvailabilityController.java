package com.krishiconnect.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.BusyRange;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Availability")
@RestController
@RequiredArgsConstructor
public class AvailabilityController {

    private final BookingService bookingService;

    @Operation(summary = "Booked time ranges for the calendar (pending requests block the time too)")
    @GetMapping("/api/equipment/{id}/availability")
    public List<BusyRange> equipment(@PathVariable Long id,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(defaultValue = "31") int days) {
        return bookingService.busy(ResourceType.EQUIPMENT, id, from, days);
    }

    @GetMapping("/api/specialists/{id}/availability")
    public List<BusyRange> specialist(@PathVariable Long id,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(defaultValue = "31") int days) {
        return bookingService.busy(ResourceType.SPECIALIST, id, from, days);
    }
}
