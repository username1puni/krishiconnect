package com.krishiconnect.service;

/**
 * Published inside the booking transaction whenever a booking changes state.
 * Listeners (notifications) run before commit, so they succeed or roll back together with it.
 */
public record BookingEvent(Long bookingId, Type type, Long itemId) {

    public enum Type {
        REQUESTED,
        ITEM_ACCEPTED,
        CONFIRMED,
        REJECTED,
        EXPIRED,
        CANCELLED,
        COMPLETED
    }
}
