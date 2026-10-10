package com.krishiconnect.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.krishiconnect.entity.BookingItem;

import lombok.RequiredArgsConstructor;

/**
 * Writes the hourly rows in availability_slots. Plain JDBC batch insert because a two week booking
 * is a few hundred rows and JPA with IDENTITY ids would insert them one by one.
 */
@Component
@RequiredArgsConstructor
public class SlotWriter {

    private final JdbcTemplate jdbcTemplate;

    /** Throws DuplicateKeyException if any hour is already taken (caught as 409 upstream). */
    public void reserve(BookingItem item) {
        List<Object[]> rows = new ArrayList<>();
        for (LocalDateTime t = item.getStartAt(); t.isBefore(item.getEndAt()); t = t.plusHours(1)) {
            rows.add(new Object[] {item.getResourceType().name(), item.resourceId(), Timestamp.valueOf(t), item.getId()});
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO availability_slots (resource_type, resource_id, slot_start, booking_item_id) VALUES (?, ?, ?, ?)",
                rows);
    }

    public void release(Collection<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return;
        }
        String in = String.join(",", itemIds.stream().map(id -> "?").toList());
        jdbcTemplate.update("DELETE FROM availability_slots WHERE booking_item_id IN (" + in + ")", itemIds.toArray());
    }
}
