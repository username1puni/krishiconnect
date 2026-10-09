package com.krishiconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.LanguageRequest;
import com.krishiconnect.dto.SpecialistProfileRequest;
import com.krishiconnect.dto.UpdateProfileRequest;
import com.krishiconnect.dto.UserResponse;
import com.krishiconnect.security.AuthUser;
import com.krishiconnect.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Profile")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return userService.me(me.getId());
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(me.getId(), request);
    }

    @PutMapping("/language")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void language(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody LanguageRequest request) {
        userService.updateLanguage(me.getId(), request.lang());
    }

    @PutMapping("/specialist")
    @PreAuthorize("hasRole('SPECIALIST')")
    public UserResponse specialist(@AuthenticationPrincipal AuthUser me,
                                   @Valid @RequestBody SpecialistProfileRequest request) {
        return userService.updateSpecialistProfile(me.getId(), request);
    }
}
