package com.krishiconnect.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.SpecialistProfile;
import com.krishiconnect.entity.User;
import com.krishiconnect.location.Distance;
import com.krishiconnect.location.DistanceSource;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpecialistCard(Long id, String fullName, Skill skill, String bio, int experienceYears,
                             BigDecimal ratePerHour, BigDecimal ratePerDay, String languages, boolean available,
                             String village, String district, Double latitude, Double longitude, boolean verified,
                             Double rating, int ratingCount, Double distanceKm, Integer durationMinutes,
                             DistanceSource distanceSource) {

    public static SpecialistCard from(SpecialistProfile p, Distance distance) {
        User u = p.getUser();
        return new SpecialistCard(u.getId(), u.getFullName(), p.getSkill(), p.getBio(), p.getExperienceYears(),
                p.getRatePerHour(), p.getRatePerDay(), p.getLanguages(), p.isAvailable(), u.getVillage(),
                u.getDistrict(), u.getLatitude(), u.getLongitude(), u.isVerified(), p.averageRating(),
                p.getRatingCount(), distance == null ? null : distance.km(),
                distance == null ? null : distance.minutes(), distance == null ? null : distance.source());
    }
}
