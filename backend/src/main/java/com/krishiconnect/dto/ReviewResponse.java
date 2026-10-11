package com.krishiconnect.dto;

import java.time.Instant;

import com.krishiconnect.entity.Review;

public record ReviewResponse(Long id, int rating, String comment, String reviewerName, String reviewerVillage,
                             Instant createdAt) {

    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getRating(), r.getComment(), r.getReviewer().getFullName(),
                r.getReviewer().getVillage(), r.getCreatedAt());
    }
}
