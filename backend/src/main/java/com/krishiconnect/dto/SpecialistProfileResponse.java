package com.krishiconnect.dto;

import java.math.BigDecimal;

import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.SpecialistProfile;

public record SpecialistProfileResponse(Skill skill, String bio, int experienceYears, BigDecimal ratePerHour,
                                        BigDecimal ratePerDay, String languages, boolean available) {

    public static SpecialistProfileResponse from(SpecialistProfile p) {
        return new SpecialistProfileResponse(p.getSkill(), p.getBio(), p.getExperienceYears(), p.getRatePerHour(),
                p.getRatePerDay(), p.getLanguages(), p.isAvailable());
    }
}
