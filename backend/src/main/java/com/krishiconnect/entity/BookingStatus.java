package com.krishiconnect.entity;

public enum BookingStatus {
    /** Waiting for every provider in the booking to accept. The time is already blocked. */
    PENDING,
    CONFIRMED,
    REJECTED,
    /** Nobody answered in time, released by the expiry job. */
    EXPIRED,
    CANCELLED,
    COMPLETED
}
