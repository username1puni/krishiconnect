package com.krishiconnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.dto.ReviewRequest;
import com.krishiconnect.dto.ReviewResponse;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.security.AuthUser;
import com.krishiconnect.service.ReviewService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "Reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(me.getId(), request);
    }

    /** Booking item ids I have already reviewed, so the app can hide the "rate" button. */
    @GetMapping("/api/reviews/mine")
    public List<Long> mine(@AuthenticationPrincipal AuthUser me) {
        return reviewService.reviewedItemIds(me.getId());
    }

    @GetMapping("/api/equipment/{id}/reviews")
    public PageResponse<ReviewResponse> equipment(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return reviewService.forResource(ResourceType.EQUIPMENT, id, page, size);
    }

    @GetMapping("/api/specialists/{id}/reviews")
    public PageResponse<ReviewResponse> specialist(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                                   @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return reviewService.forResource(ResourceType.SPECIALIST, id, page, size);
    }
}
