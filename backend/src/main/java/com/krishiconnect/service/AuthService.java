package com.krishiconnect.service;

import java.math.BigDecimal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.krishiconnect.dto.AuthResponse;
import com.krishiconnect.dto.LoginRequest;
import com.krishiconnect.dto.RegisterRequest;
import com.krishiconnect.dto.UserResponse;
import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.SpecialistProfile;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.BadRequestException;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.repository.SpecialistProfileRepository;
import com.krishiconnect.repository.UserRepository;
import com.krishiconnect.security.AuthUser;
import com.krishiconnect.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SpecialistProfileRepository specialistProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts can't be created from the sign up page");
        }
        String email = StringUtils.hasText(request.email()) ? request.email().trim().toLowerCase() : null;
        if (userRepository.existsByPhone(request.phone())) {
            throw new ConflictException("An account with this phone number already exists");
        }
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User(request.fullName().trim(), request.phone(), email,
                passwordEncoder.encode(request.password()), request.role());
        user.setVillage(request.village());
        user.setDistrict(request.district());
        user.setLatitude(request.latitude());
        user.setLongitude(request.longitude());
        user.setPreferredLang(request.preferredLang() == null ? "en" : request.preferredLang());
        // seekers need no checks; providers and specialists wait for an admin
        user.setVerified(request.role() == Role.SEEKER);
        userRepository.save(user);

        SpecialistProfile profile = null;
        if (request.role() == Role.SPECIALIST) {
            profile = new SpecialistProfile(user, request.skill() == null ? Skill.FARM_LABOUR : request.skill());
            profile.setRatePerDay(BigDecimal.valueOf(800));
            profile = specialistProfileRepository.save(profile);
        }
        return tokenFor(user, profile);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String id = request.identifier().trim();
        User user = (id.contains("@") ? userRepository.findByEmailIgnoreCase(id) : userRepository.findByPhone(id))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadRequestException("Wrong phone/email or password"));
        if (!user.isEnabled()) {
            throw new BadRequestException("This account has been blocked. Please contact support.");
        }
        SpecialistProfile profile = user.getRole() == Role.SPECIALIST
                ? specialistProfileRepository.findById(user.getId()).orElse(null) : null;
        return tokenFor(user, profile);
    }

    private AuthResponse tokenFor(User user, SpecialistProfile profile) {
        String token = jwtService.createToken(new AuthUser(user));
        return new AuthResponse(token, jwtService.expirySeconds(), UserResponse.from(user, profile));
    }
}
