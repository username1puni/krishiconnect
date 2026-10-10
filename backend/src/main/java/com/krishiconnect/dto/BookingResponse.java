package com.krishiconnect.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishiconnect.entity.Booking;
import com.krishiconnect.entity.BookingKind;
import com.krishiconnect.entity.BookingStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingResponse(Long id, BookingKind kind, BookingStatus status, LocalDateTime startAt,
                              LocalDateTime endAt, BigDecimal totalAmount, String note, Instant expiresAt,
                              Instant createdAt, Instant confirmedAt, Long seekerId, String seekerName,
                              String seekerPhone, String seekerVillage, List<BookingItemResponse> items) {

    public static BookingResponse from(Booking b) {
        boolean confirmed = b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED;
        var seeker = b.getSeeker();
        return new BookingResponse(b.getId(), b.getKind(), b.getStatus(), b.getStartAt(), b.getEndAt(),
                b.getTotalAmount(), b.getNote(), b.getExpiresAt(), b.getCreatedAt(), b.getConfirmedAt(),
                seeker.getId(), seeker.getFullName(), confirmed ? seeker.getPhone() : null, seeker.getVillage(),
                b.getItems().stream().map(i -> BookingItemResponse.from(i, confirmed)).toList());
    }
}
