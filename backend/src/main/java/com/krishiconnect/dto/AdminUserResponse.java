package com.krishiconnect.dto;

import java.time.Instant;

import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.User;

public record AdminUserResponse(Long id, String fullName, String phone, String email, Role role, String village,
                                String district, boolean verified, boolean enabled, Instant createdAt) {

    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getFullName(), u.getPhone(), u.getEmail(), u.getRole(),
                u.getVillage(), u.getDistrict(), u.isVerified(), u.isEnabled(), u.getCreatedAt());
    }
}
