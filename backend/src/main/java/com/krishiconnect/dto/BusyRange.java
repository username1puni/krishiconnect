package com.krishiconnect.dto;

import java.time.LocalDateTime;

import com.krishiconnect.entity.BookingItemStatus;

/** A time range when equipment or a specialist is taken, for the availability calendar. */
public record BusyRange(LocalDateTime startAt, LocalDateTime endAt, BookingItemStatus status) {
}
