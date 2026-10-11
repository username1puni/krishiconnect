package com.krishiconnect.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.AdminUserResponse;
import com.krishiconnect.dto.BookingResponse;
import com.krishiconnect.dto.DashboardResponse;
import com.krishiconnect.dto.EquipmentDetail;
import com.krishiconnect.dto.ModerationRequest;
import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.dto.ToggleRequest;
import com.krishiconnect.entity.BookingStatus;
import com.krishiconnect.entity.ModerationStatus;
import com.krishiconnect.entity.Role;
import com.krishiconnect.location.DistanceService;
import com.krishiconnect.location.RoutingStatus;
import com.krishiconnect.service.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/** Everything here needs ROLE_ADMIN (see SecurityConfig). */
@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final DistanceService distanceService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return adminService.dashboard();
    }

    @GetMapping("/users")
    public PageResponse<AdminUserResponse> users(@RequestParam(required = false) Role role,
                                                 @RequestParam(required = false) Boolean verified,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.users(role, verified, q, page, size);
    }

    @PatchMapping("/users/{id}/verify")
    public AdminUserResponse verify(@PathVariable Long id, @Valid @RequestBody ToggleRequest request) {
        return adminService.setVerified(id, request.value());
    }

    @PatchMapping("/users/{id}/enabled")
    public AdminUserResponse enabled(@PathVariable Long id, @Valid @RequestBody ToggleRequest request) {
        return adminService.setEnabled(id, request.value());
    }

    @GetMapping("/equipment")
    public PageResponse<EquipmentDetail> listings(@RequestParam(required = false) ModerationStatus status,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.listings(status, page, size);
    }

    @PatchMapping("/equipment/{id}/moderation")
    public EquipmentDetail moderate(@PathVariable Long id, @Valid @RequestBody ModerationRequest request) {
        return adminService.moderate(id, request.status(), request.note());
    }

    @GetMapping("/bookings")
    public PageResponse<BookingResponse> bookings(@RequestParam(required = false) BookingStatus status,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminService.bookings(status, page, size);
    }

    @GetMapping("/routing")
    public RoutingStatus routing() {
        return distanceService.status();
    }

    @Operation(summary = "Turn the road distance API on or off (off = straight line distance everywhere)")
    @PutMapping("/routing")
    public RoutingStatus setRouting(@Valid @RequestBody ToggleRequest request) {
        distanceService.setEnabled(request.value());
        return distanceService.status();
    }

    @PostMapping("/routing/clear-cache")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> clearCache() {
        distanceService.clearCache();
        return Map.of("status", "cleared");
    }
}
