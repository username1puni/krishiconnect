package com.krishiconnect.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.NearbyPageResponse;
import com.krishiconnect.dto.SpecialistCard;
import com.krishiconnect.entity.Skill;
import com.krishiconnect.security.AuthUser;
import com.krishiconnect.service.SpecialistService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "Specialists")
@RestController
@RequestMapping("/api/specialists")
@RequiredArgsConstructor
public class SpecialistController {

    private final SpecialistService specialistService;

    @GetMapping
    public NearbyPageResponse<SpecialistCard> search(@RequestParam(required = false) Double lat,
                                                     @RequestParam(required = false) Double lng,
                                                     @RequestParam(required = false) Double radiusKm,
                                                     @RequestParam(required = false) Skill skill,
                                                     @RequestParam(defaultValue = "distance") String sort,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return specialistService.search(new SpecialistService.SearchQuery(lat, lng, radiusKm, skill, sort), page, size);
    }

    @GetMapping("/{id}")
    public SpecialistCard get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        return specialistService.get(id, me);
    }
}
