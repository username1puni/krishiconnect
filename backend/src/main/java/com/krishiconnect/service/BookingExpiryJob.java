package com.krishiconnect.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Releases booking requests that no provider answered in time, so the tractor doesn't stay
 * blocked by a request nobody is going to accept.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryJob {

    private static final int BATCH = 100;

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${app.booking.expiry-check-ms}", initialDelayString = "${app.booking.expiry-check-ms}")
    public void run() {
        List<Long> ids = bookingService.findExpiredIds(BATCH);
        int expired = 0;
        for (Long id : ids) {
            try {
                // each booking in its own transaction, one failure doesn't stop the rest
                if (bookingService.expire(id)) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.warn("Could not expire booking {}: {}", id, e.getMessage());
            }
        }
        if (expired > 0) {
            log.info("Expired {} booking request(s)", expired);
        }
    }
}
