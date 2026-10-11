package com.krishiconnect.support;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishiconnect.dto.BookingItemRequest;
import com.krishiconnect.dto.CreateBookingRequest;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.ModerationStatus;
import com.krishiconnect.entity.PriceUnit;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.SpecialistProfile;
import com.krishiconnect.entity.User;
import com.krishiconnect.repository.EquipmentRepository;
import com.krishiconnect.repository.SpecialistProfileRepository;
import com.krishiconnect.repository.UserRepository;

/** Builds the few rows a test needs. Phone numbers are unique per run so tests never clash. */
@TestComponent
public class TestData {

    public static final String PASSWORD = "secret123";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private SpecialistProfileRepository specialistRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Clock clock;
    @Autowired
    private TransactionTemplate tx;

    private final AtomicInteger phones = new AtomicInteger(100_000);
    private String passwordHash;

    public void clean() {
        for (String table : List.of("availability_slots", "reviews", "quotes", "requirements", "notifications",
                "booking_items", "bookings", "equipment_photos", "equipment", "specialist_profiles")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("DELETE FROM users WHERE role <> 'ADMIN'");
    }

    public User user(String name, Role role) {
        if (passwordHash == null) {
            passwordHash = passwordEncoder.encode(PASSWORD);
        }
        String phone = "9" + String.format("%09d", phones.incrementAndGet());
        User u = new User(name, phone, null, passwordHash, role);
        u.setVillage("Mandya");
        u.setDistrict("Mandya");
        u.setLatitude(12.5223);
        u.setLongitude(76.8970);
        u.setVerified(true);
        return userRepository.save(u);
    }

    public User seeker(String name) {
        return user(name, Role.SEEKER);
    }

    public User provider(String name) {
        return user(name, Role.PROVIDER);
    }

    public User specialist(String name, Skill skill) {
        User saved = user(name, Role.SPECIALIST);
        // the profile shares the user's id (@MapsId), so it needs the managed user in the same transaction
        tx.executeWithoutResult(status -> {
            SpecialistProfile p = new SpecialistProfile(userRepository.findById(saved.getId()).orElseThrow(), skill);
            p.setRatePerHour(BigDecimal.valueOf(150));
            p.setRatePerDay(BigDecimal.valueOf(900));
            specialistRepository.save(p);
        });
        return saved;
    }

    public Equipment tractor(User owner) {
        return equipment(owner, "Swaraj 744 FE", EquipmentCategory.TRACTOR, 12.5223, 76.8970);
    }

    public Equipment equipment(User owner, String title, EquipmentCategory category, double lat, double lng) {
        Equipment e = new Equipment();
        e.setOwner(owner);
        e.setTitle(title);
        e.setCategory(category);
        e.setPricePerHour(BigDecimal.valueOf(900));
        e.setPricePerDay(BigDecimal.valueOf(6500));
        e.setPricePerAcre(BigDecimal.valueOf(1200));
        e.setWithOperator(true);
        e.setVillage("Mandya");
        e.setLatitude(lat);
        e.setLongitude(lng);
        e.setModerationStatus(ModerationStatus.APPROVED);
        return equipmentRepository.save(e);
    }

    /** Tomorrow (or later) at the given hour, local time. */
    public LocalDateTime day(int daysAhead, int hour) {
        return LocalDateTime.now(clock).toLocalDate().plusDays(daysAhead).atTime(hour, 0);
    }

    public CreateBookingRequest equipmentBooking(Long equipmentId, LocalDateTime start, LocalDateTime end) {
        return new CreateBookingRequest(List.of(new BookingItemRequest(ResourceType.EQUIPMENT, equipmentId,
                PriceUnit.HOUR, null)), start, end, null);
    }

    public CreateBookingRequest bundle(Long equipmentId, Long specialistId, LocalDateTime start, LocalDateTime end) {
        return new CreateBookingRequest(List.of(
                new BookingItemRequest(ResourceType.EQUIPMENT, equipmentId, PriceUnit.HOUR, null),
                new BookingItemRequest(ResourceType.SPECIALIST, specialistId, PriceUnit.DAY, null)), start, end, null);
    }

    public String string(String sql, Object... args) {
        return jdbc.queryForObject(sql, String.class, args);
    }

    public int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
