package com.krishiconnect.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.krishiconnect.entity.Booking;
import com.krishiconnect.entity.BookingItem;
import com.krishiconnect.entity.NotificationType;
import com.krishiconnect.entity.Quote;
import com.krishiconnect.repository.BookingRepository;
import com.krishiconnect.repository.QuoteRepository;

import lombok.RequiredArgsConstructor;

/**
 * Turns booking and quote events into in-app notifications. Runs just before the transaction
 * commits, inside it: if writing the notification fails, the booking change rolls back too.
 */
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH);

    private final NotificationService notifications;
    private final BookingRepository bookingRepository;
    private final QuoteRepository quoteRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onBooking(BookingEvent event) {
        Booking b = bookingRepository.findById(event.bookingId()).orElse(null);
        if (b == null) {
            return;
        }
        String when = range(b.getStartAt(), b.getEndAt());
        String seeker = b.getSeeker().getFullName();
        BookingItem item = event.itemId() == null ? null
                : b.getItems().stream().filter(i -> i.getId().equals(event.itemId())).findFirst().orElse(null);

        switch (event.type()) {
            case REQUESTED -> {
                for (BookingItem i : b.getItems()) {
                    notifications.notify(i.getProvider().getId(), NotificationType.BOOKING_REQUESTED,
                            "New booking request",
                            seeker + " wants " + i.resourceName() + " on " + when + ". Please accept or reject.",
                            "/provider/bookings", data(b, i, seeker, when));
                }
            }
            case ITEM_ACCEPTED -> notifications.notify(b.getSeeker().getId(), NotificationType.BOOKING_ITEM_ACCEPTED,
                    "Part of your booking was accepted",
                    item.getProvider().getFullName() + " accepted " + item.resourceName()
                            + ". Waiting for the rest of your bundle.",
                    "/bookings", data(b, item, item.getProvider().getFullName(), when));
            case CONFIRMED -> notifications.notify(b.getSeeker().getId(), NotificationType.BOOKING_CONFIRMED,
                    "Booking confirmed",
                    "Your booking #" + b.getId() + " for " + names(b) + " on " + when + " is confirmed.",
                    "/bookings", data(b, item, seeker, when));
            case REJECTED -> {
                String by = item == null ? "The provider" : item.getProvider().getFullName();
                notifications.notify(b.getSeeker().getId(), NotificationType.BOOKING_REJECTED, "Booking rejected",
                        by + " could not take booking #" + b.getId() + " (" + names(b) + ", " + when
                                + "). Nothing was booked.",
                        "/bookings", data(b, item, by, when));
                notifyOtherProviders(b, item, NotificationType.BOOKING_REJECTED, "Booking request withdrawn",
                        "Booking #" + b.getId() + " was cancelled because another part of the bundle was turned down.",
                        when);
            }
            case EXPIRED -> {
                notifications.notify(b.getSeeker().getId(), NotificationType.BOOKING_EXPIRED, "Booking request expired",
                        "Booking #" + b.getId() + " (" + names(b) + ", " + when
                                + ") expired because it was not accepted in time. The dates are free again.",
                        "/bookings", data(b, null, seeker, when));
                notifyOtherProviders(b, null, NotificationType.BOOKING_EXPIRED, "Booking request expired",
                        "The request from " + seeker + " for " + when + " expired before it was accepted.", when);
            }
            case CANCELLED -> notifyOtherProviders(b, null, NotificationType.BOOKING_CANCELLED, "Booking cancelled",
                    seeker + " cancelled booking #" + b.getId() + " for " + when + ".", when);
            case COMPLETED -> notifications.notify(b.getSeeker().getId(), NotificationType.BOOKING_COMPLETED,
                    "Work completed",
                    "Booking #" + b.getId() + " is marked as done. Please rate " + names(b) + ".",
                    "/bookings", data(b, null, seeker, when));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onQuote(QuoteEvent event) {
        Quote q = quoteRepository.findById(event.quoteId()).orElse(null);
        if (q == null) {
            return;
        }
        var r = q.getRequirement();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requirementId", r.getId());
        data.put("title", r.getTitle());
        data.put("amount", q.getAmount());
        switch (event.type()) {
            case RECEIVED -> {
                data.put("name", q.getProvider().getFullName());
                notifications.notify(r.getSeeker().getId(), NotificationType.QUOTE_RECEIVED, "New quote",
                        q.getProvider().getFullName() + " quoted Rs " + q.getAmount().toPlainString() + " for \""
                                + r.getTitle() + "\".",
                        "/requirements/" + r.getId(), data);
            }
            case ACCEPTED -> {
                data.put("name", r.getSeeker().getFullName());
                data.put("bookingId", q.getBookingId());
                notifications.notify(q.getProvider().getId(), NotificationType.QUOTE_ACCEPTED, "Quote accepted",
                        r.getSeeker().getFullName() + " accepted your quote for \"" + r.getTitle()
                                + "\". Booking #" + q.getBookingId() + " is confirmed.",
                        "/provider/bookings", data);
            }
            case REJECTED -> notifications.notify(q.getProvider().getId(), NotificationType.QUOTE_REJECTED,
                    "Quote not selected", "Your quote for \"" + r.getTitle() + "\" was not selected this time.",
                    "/requirements/" + r.getId(), data);
        }
    }

    private void notifyOtherProviders(Booking b, BookingItem except, NotificationType type, String title,
                                      String message, String when) {
        b.getItems().stream()
                .filter(i -> except == null || !i.getId().equals(except.getId()))
                .map(i -> i.getProvider().getId())
                .distinct()
                .forEach(id -> notifications.notify(id, type, title, message, "/provider/bookings",
                        data(b, null, b.getSeeker().getFullName(), when)));
    }

    private static Map<String, Object> data(Booking b, BookingItem item, String name, String when) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bookingId", b.getId());
        data.put("name", name);
        data.put("when", when);
        data.put("resource", item != null ? item.resourceName() : names(b));
        return data;
    }

    private static String names(Booking b) {
        return String.join(" + ", b.getItems().stream().map(BookingItem::resourceName).filter(Objects::nonNull).toList());
    }

    static String range(LocalDateTime start, LocalDateTime end) {
        if (start.toLocalDate().equals(end.toLocalDate())) {
            return DAY.format(start) + ", " + HOUR.format(start) + " to " + HOUR.format(end);
        }
        return DAY.format(start) + " " + HOUR.format(start) + " to " + DAY.format(end) + " " + HOUR.format(end);
    }
}
