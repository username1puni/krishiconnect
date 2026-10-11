package com.krishiconnect.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.RecommendationResponse;
import com.krishiconnect.security.AuthUser;
import com.krishiconnect.service.RecommendationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "Seasonal recommendations")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "What farmers need this month (or the given month). Logged in users see their own crops first.")
    @GetMapping
    public List<RecommendationResponse> list(@RequestParam(required = false) @Min(1) @Max(12) Integer month,
                                             @RequestParam(required = false) String crop,
                                             @RequestParam(required = false) Double lat,
                                             @RequestParam(required = false) Double lng,
                                             @AuthenticationPrincipal AuthUser me) {
        return recommendationService.forMonth(month, crop, lat, lng, me == null ? null : me.getId());
    }

    @GetMapping("/crops")
    public List<String> crops() {
        return recommendationService.crops();
    }
}
