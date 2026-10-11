package com.krishiconnect.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.dto.ReviewRequest;
import com.krishiconnect.dto.ReviewResponse;
import com.krishiconnect.entity.BookingItem;
import com.krishiconnect.entity.BookingItemStatus;
import com.krishiconnect.entity.NotificationType;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Review;
import com.krishiconnect.exception.BadRequestException;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.exception.ForbiddenException;
import com.krishiconnect.exception.NotFoundException;
import com.krishiconnect.repository.BookingItemRepository;
import com.krishiconnect.repository.RatingRepository;
import com.krishiconnect.repository.ReviewRepository;
import com.krishiconnect.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingItemRepository itemRepository;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final NotificationService notifications;

    /** Only the farmer who booked, and only after the provider marked the work as done. */
    @Transactional
    public ReviewResponse create(Long reviewerId, ReviewRequest request) {
        BookingItem item = itemRepository.findById(request.bookingItemId())
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!item.getBooking().getSeeker().getId().equals(reviewerId)) {
            throw new ForbiddenException("You can only review your own bookings");
        }
        if (item.getStatus() != BookingItemStatus.COMPLETED) {
            throw new BadRequestException("You can review once the work is marked as completed");
        }
        if (reviewRepository.existsByBookingItemId(item.getId())) {
            throw new ConflictException("You have already reviewed this booking");
        }
        Review review = new Review();
        review.setBookingItemId(item.getId());
        review.setReviewer(userRepository.getReferenceById(reviewerId));
        review.setResourceType(item.getResourceType());
        review.setRating(request.rating());
        review.setComment(request.comment());
        // a plain UPDATE ... SET sum = sum + ? is atomic, no need to lock the equipment row
        if (item.getResourceType() == ResourceType.EQUIPMENT) {
            review.setEquipmentId(item.getEquipment().getId());
            ratingRepository.addEquipmentRating(item.getEquipment().getId(), request.rating());
        } else {
            review.setSpecialistId(item.getSpecialist().getId());
            ratingRepository.addSpecialistRating(item.getSpecialist().getId(), request.rating());
        }
        // flush now so a duplicate sent at the same moment fails on the unique key (409), not at commit
        reviewRepository.saveAndFlush(review);

        String who = item.getBooking().getSeeker().getFullName();
        notifications.notify(item.getProvider().getId(), NotificationType.REVIEW_RECEIVED, "New review",
                who + " rated " + item.resourceName() + " " + request.rating() + " out of 5.",
                item.getResourceType() == ResourceType.EQUIPMENT ? "/equipment/" + item.getEquipment().getId()
                        : "/specialists/" + item.getSpecialist().getId(),
                Map.of("name", who, "resource", item.resourceName(), "rating", request.rating()));
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> forResource(ResourceType type, Long id, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = type == ResourceType.EQUIPMENT ? reviewRepository.findByEquipment(id, pageable)
                : reviewRepository.findBySpecialist(id, pageable);
        return PageResponse.of(result, ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public List<Long> reviewedItemIds(Long reviewerId) {
        return reviewRepository.findReviewedItemIds(reviewerId);
    }
}
