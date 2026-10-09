package com.krishiconnect.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.krishiconnect.dto.SpecialistProfileRequest;
import com.krishiconnect.dto.UpdateProfileRequest;
import com.krishiconnect.dto.UserResponse;
import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.SpecialistProfile;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.exception.ForbiddenException;
import com.krishiconnect.exception.NotFoundException;
import com.krishiconnect.repository.SpecialistProfileRepository;
import com.krishiconnect.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SpecialistProfileRepository specialistProfileRepository;

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        User user = get(userId);
        return UserResponse.from(user, specialistProfileRepository.findById(userId).orElse(null));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = get(userId);
        String email = StringUtils.hasText(request.email()) ? request.email().trim().toLowerCase() : null;
        if (email != null && !email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Another account already uses this email");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setVillage(request.village());
        user.setDistrict(request.district());
        user.setLatitude(request.latitude());
        user.setLongitude(request.longitude());
        if (request.preferredLang() != null) {
            user.setPreferredLang(request.preferredLang());
        }
        user.setCrops(request.crops() == null ? null : String.join(",", request.crops()));
        return UserResponse.from(user, specialistProfileRepository.findById(userId).orElse(null));
    }

    @Transactional
    public void updateLanguage(Long userId, String lang) {
        get(userId).setPreferredLang(lang);
    }

    @Transactional
    public UserResponse updateSpecialistProfile(Long userId, SpecialistProfileRequest request) {
        User user = get(userId);
        if (user.getRole() != Role.SPECIALIST) {
            throw new ForbiddenException("Only specialists have a work profile");
        }
        SpecialistProfile profile = specialistProfileRepository.findById(userId)
                .orElseGet(() -> new SpecialistProfile(user, request.skill()));
        profile.setSkill(request.skill());
        profile.setBio(request.bio());
        profile.setExperienceYears(request.experienceYears());
        profile.setRatePerHour(request.ratePerHour());
        profile.setRatePerDay(request.ratePerDay());
        profile.setLanguages(request.languages());
        profile.setAvailable(request.available());
        profile = specialistProfileRepository.save(profile);
        return UserResponse.from(user, profile);
    }

    User get(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
