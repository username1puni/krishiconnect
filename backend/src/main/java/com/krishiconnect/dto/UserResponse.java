package com.krishiconnect.dto;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.SpecialistProfile;
import com.krishiconnect.entity.User;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(Long id, String fullName, String phone, String email, Role role, String village,
                           String district, Double latitude, Double longitude, String preferredLang,
                           List<String> crops, boolean verified, Instant createdAt,
                           SpecialistProfileResponse specialist) {

    public static UserResponse from(User u) {
        return from(u, null);
    }

    public static UserResponse from(User u, SpecialistProfile profile) {
        List<String> crops = u.getCrops() == null || u.getCrops().isBlank() ? List.of()
                : Arrays.stream(u.getCrops().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new UserResponse(u.getId(), u.getFullName(), u.getPhone(), u.getEmail(), u.getRole(), u.getVillage(),
                u.getDistrict(), u.getLatitude(), u.getLongitude(), u.getPreferredLang(), crops, u.isVerified(),
                u.getCreatedAt(), profile == null ? null : SpecialistProfileResponse.from(profile));
    }
}
