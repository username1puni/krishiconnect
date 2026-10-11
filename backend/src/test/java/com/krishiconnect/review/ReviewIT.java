package com.krishiconnect.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishiconnect.dto.BookingResponse;
import com.krishiconnect.dto.ReviewRequest;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.BadRequestException;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.exception.ForbiddenException;
import com.krishiconnect.service.BookingService;
import com.krishiconnect.service.ReviewService;
import com.krishiconnect.support.IntegrationTest;

class ReviewIT extends IntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ReviewService reviewService;

    @Test
    void reviewOnlyAfterTheWorkIsDoneAndOnlyOnce() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User farmer = data.seeker("Farmer");
        User stranger = data.seeker("Stranger");
        LocalDateTime six = data.day(1, 6);
        BookingResponse b = bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(4)));
        Long itemId = b.items().get(0).id();
        bookingService.acceptItem(owner.getId(), itemId);

        assertThatThrownBy(() -> reviewService.create(farmer.getId(), new ReviewRequest(itemId, 5, "Good")))
                .isInstanceOf(BadRequestException.class);

        clock.advance(Duration.ofDays(2));
        bookingService.completeItem(owner.getId(), itemId);

        assertThatThrownBy(() -> reviewService.create(stranger.getId(), new ReviewRequest(itemId, 1, "Bad")))
                .isInstanceOf(ForbiddenException.class);
        reviewService.create(farmer.getId(), new ReviewRequest(itemId, 4, "Came on time"));
        assertThatThrownBy(() -> reviewService.create(farmer.getId(), new ReviewRequest(itemId, 5, "Again")))
                .isInstanceOf(ConflictException.class);

        assertThat(data.count("SELECT rating_sum FROM equipment WHERE id = ?", tractor.getId())).isEqualTo(4);
        assertThat(data.count("SELECT rating_count FROM equipment WHERE id = ?", tractor.getId())).isEqualTo(1);
        assertThat(data.count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'REVIEW_RECEIVED'",
                owner.getId())).isEqualTo(1);
        assertThat(reviewService.reviewedItemIds(farmer.getId())).containsExactly(itemId);
    }
}
