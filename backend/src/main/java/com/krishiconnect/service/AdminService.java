package com.krishiconnect.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.krishiconnect.dto.AdminUserResponse;
import com.krishiconnect.dto.BookingResponse;
import com.krishiconnect.dto.DashboardResponse;
import com.krishiconnect.dto.DashboardResponse.CategoryCount;
import com.krishiconnect.dto.DashboardResponse.CategoryMonth;
import com.krishiconnect.dto.DashboardResponse.MonthStat;
import com.krishiconnect.dto.EquipmentDetail;
import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.entity.BookingStatus;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.ModerationStatus;
import com.krishiconnect.entity.NotificationType;
import com.krishiconnect.entity.RequirementStatus;
import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.BadRequestException;
import com.krishiconnect.exception.NotFoundException;
import com.krishiconnect.location.DistanceService;
import com.krishiconnect.repository.BookingRepository;
import com.krishiconnect.repository.EquipmentPhotoRepository;
import com.krishiconnect.repository.EquipmentRepository;
import com.krishiconnect.repository.RequirementRepository;
import com.krishiconnect.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentPhotoRepository photoRepository;
    private final BookingRepository bookingRepository;
    private final RequirementRepository requirementRepository;
    private final NotificationService notifications;
    private final DistanceService distanceService;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Map<String, Long> users = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            users.put(role.name(), userRepository.countByRole(role));
        }
        Map<String, Long> bookings = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            bookings.put(status.name(), bookingRepository.countByStatus(status));
        }
        LocalDate from = YearMonth.now(clock).minusMonths(11).atDay(1);

        // reporting queries are plain SQL, easier to read than JPQL for GROUP BY month
        List<MonthStat> monthly = jdbc.query("""
                SELECT DATE_FORMAT(b.start_at, '%Y-%m') AS m, COUNT(*) AS n, COALESCE(SUM(b.total_amount), 0) AS v
                FROM bookings b
                WHERE b.status IN ('CONFIRMED', 'COMPLETED') AND b.start_at >= ?
                GROUP BY m ORDER BY m
                """, (rs, i) -> new MonthStat(rs.getString("m"), rs.getLong("n"), rs.getBigDecimal("v")), from);

        List<CategoryMonth> seasonal = jdbc.query("""
                SELECT DATE_FORMAT(i.start_at, '%Y-%m') AS m, e.category AS c, COUNT(*) AS n
                FROM booking_items i
                JOIN equipment e ON e.id = i.equipment_id
                WHERE i.status IN ('ACCEPTED', 'COMPLETED') AND i.start_at >= ?
                GROUP BY m, c ORDER BY m, c
                """, (rs, i) -> new CategoryMonth(rs.getString("m"), rs.getString("c"), rs.getLong("n")), from);

        List<CategoryCount> top = jdbc.query("""
                SELECT e.category AS c, COUNT(*) AS n
                FROM booking_items i
                JOIN equipment e ON e.id = i.equipment_id
                WHERE i.status IN ('ACCEPTED', 'COMPLETED')
                GROUP BY c ORDER BY n DESC LIMIT 6
                """, (rs, i) -> new CategoryCount(rs.getString("c"), rs.getLong("n")));

        return new DashboardResponse(users, userRepository.countPendingVerification(), equipmentRepository.count(),
                equipmentRepository.countByModerationStatus(ModerationStatus.PENDING),
                requirementRepository.countByStatus(RequirementStatus.OPEN), bookings,
                bookingRepository.sumBookedValue(), monthly, seasonal, top, distanceService.status());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(Role role, Boolean verified, String q, int page, int size) {
        String query = q == null || q.isBlank() ? null : q.trim();
        var result = userRepository.search(role, verified, query,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse setVerified(Long userId, boolean verified) {
        User user = user(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admins don't need verification");
        }
        boolean changed = user.isVerified() != verified;
        user.setVerified(verified);
        if (changed && verified) {
            notifications.notify(user.getId(), NotificationType.ACCOUNT_VERIFIED, "Account verified",
                    "Your account has been verified. Farmers can now find and book you.",
                    user.getRole() == Role.PROVIDER ? "/provider/equipment" : "/profile", Map.of("name", user.getFullName()));
        }
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse setEnabled(Long userId, boolean enabled) {
        User user = user(userId);
        if (user.getRole() == Role.ADMIN && !enabled) {
            throw new BadRequestException("Admin accounts can't be blocked here");
        }
        user.setEnabled(enabled);
        return AdminUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentDetail> listings(ModerationStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = status == null ? equipmentRepository.findAllWithOwner(pageable)
                : equipmentRepository.findByModeration(status, pageable);
        return PageResponse.of(result, e -> EquipmentDetail.from(e, photoRepository.findIdsByEquipmentId(e.getId())));
    }

    @Transactional
    public EquipmentDetail moderate(Long equipmentId, ModerationStatus status, String note) {
        Equipment e = equipmentRepository.findWithOwner(equipmentId)
                .orElseThrow(() -> new NotFoundException("Equipment " + equipmentId + " not found"));
        e.setModerationStatus(status);
        e.setModerationNote(note);
        if (status != ModerationStatus.PENDING) {
            boolean approved = status == ModerationStatus.APPROVED;
            notifications.notify(e.getOwner().getId(),
                    approved ? NotificationType.LISTING_APPROVED : NotificationType.LISTING_REJECTED,
                    approved ? "Listing approved" : "Listing needs changes",
                    approved ? "\"" + e.getTitle() + "\" is now live and farmers can book it."
                            : "\"" + e.getTitle() + "\" was not approved" + (note == null ? "." : ": " + note),
                    "/provider/equipment", Map.of("title", e.getTitle(), "note", note == null ? "" : note));
        }
        return EquipmentDetail.from(e, photoRepository.findIdsByEquipmentId(equipmentId));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> bookings(BookingStatus status, int page, int size) {
        var result = bookingRepository.findAllWithSeeker(status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, BookingResponse::from);
    }

    private User user(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }
}
