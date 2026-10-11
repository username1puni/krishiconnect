package com.krishiconnect.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishiconnect.dto.BookingResponse;
import com.krishiconnect.entity.BookingItemStatus;
import com.krishiconnect.entity.BookingStatus;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.service.BookingExpiryJob;
import com.krishiconnect.service.BookingService;
import com.krishiconnect.support.IntegrationTest;

class BookingLifecycleIT extends IntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingExpiryJob expiryJob;

    @Test
    void providerAcceptsAndFarmerIsNotified() {
        User owner = data.provider("Manjunath");
        Equipment tractor = data.tractor(owner);
        User farmer = data.seeker("Ramesh");
        LocalDateTime six = data.day(2, 6);

        BookingResponse booking = bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(5)));
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.totalAmount()).isEqualByComparingTo("4500");
        assertThat(notifications(owner, "BOOKING_REQUESTED")).isEqualTo(1);

        BookingResponse accepted = bookingService.acceptItem(owner.getId(), booking.items().get(0).id());
        assertThat(accepted.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(accepted.items().get(0).providerPhone()).isEqualTo(owner.getPhone());
        assertThat(notifications(farmer, "BOOKING_CONFIRMED")).isEqualTo(1);
    }

    @Test
    void bundleIsConfirmedOnlyWhenEveryoneAccepts() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User driver = data.specialist("Driver", Skill.TRACTOR_DRIVER);
        User farmer = data.seeker("Farmer");
        LocalDateTime six = data.day(2, 6);

        BookingResponse b = bookingService.create(farmer.getId(), data.bundle(tractor.getId(), driver.getId(), six, six.plusHours(8)));
        assertThat(b.totalAmount()).isEqualByComparingTo("8100"); // 8 h x 900 + 1 day x 900

        BookingResponse afterOwner = bookingService.acceptItem(owner.getId(), b.items().get(0).id());
        assertThat(afterOwner.status()).isEqualTo(BookingStatus.PENDING);
        BookingResponse afterDriver = bookingService.acceptItem(driver.getId(), b.items().get(1).id());
        assertThat(afterDriver.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void oneRejectionReleasesTheWholeBundle() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User driver = data.specialist("Driver", Skill.TRACTOR_DRIVER);
        User farmer = data.seeker("Farmer");
        User other = data.seeker("Other");
        LocalDateTime six = data.day(2, 6);

        BookingResponse b = bookingService.create(farmer.getId(), data.bundle(tractor.getId(), driver.getId(), six, six.plusHours(8)));
        bookingService.acceptItem(owner.getId(), b.items().get(0).id());
        BookingResponse rejected = bookingService.rejectItem(driver.getId(), b.items().get(1).id());

        assertThat(rejected.status()).isEqualTo(BookingStatus.REJECTED);
        assertThat(rejected.items()).extracting(i -> i.status()).containsOnly(BookingItemStatus.REJECTED);
        assertThat(data.count("SELECT COUNT(*) FROM availability_slots")).isZero();
        assertThat(notifications(farmer, "BOOKING_REJECTED")).isEqualTo(1);
        assertThat(notifications(owner, "BOOKING_REJECTED")).isEqualTo(1);
        // the tractor is free again
        assertThat(bookingService.create(other.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(8))).status())
                .isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void unansweredRequestExpiresAndFreesTheDates() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User farmer = data.seeker("Farmer");
        User other = data.seeker("Other");
        LocalDateTime six = data.day(3, 6);
        BookingResponse b = bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(4)));

        expiryJob.run();
        assertThat(status(b.id())).isEqualTo("PENDING");

        clock.advance(Duration.ofMinutes(31));
        expiryJob.run();

        assertThat(status(b.id())).isEqualTo("EXPIRED");
        assertThat(data.count("SELECT COUNT(*) FROM availability_slots")).isZero();
        assertThat(notifications(farmer, "BOOKING_EXPIRED")).isEqualTo(1);
        assertThat(notifications(owner, "BOOKING_EXPIRED")).isEqualTo(1);
        // accepting late is refused
        assertThatThrownBy(() -> bookingService.acceptItem(owner.getId(), b.items().get(0).id()))
                .isInstanceOf(ConflictException.class);
        // and someone else can book the same hours now
        assertThat(bookingService.create(other.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(4))).status())
                .isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void acceptedBookingIsNotExpired() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User farmer = data.seeker("Farmer");
        LocalDateTime six = data.day(3, 6);
        BookingResponse b = bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(4)));
        bookingService.acceptItem(owner.getId(), b.items().get(0).id());

        clock.advance(Duration.ofHours(2));
        expiryJob.run();

        assertThat(status(b.id())).isEqualTo("CONFIRMED");
    }

    @Test
    void pendingRequestNeverOutlivesTheStartTime() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User farmer = data.seeker("Farmer");
        // starts in less than the 30 minute timeout
        LocalDateTime soon = LocalDateTime.now(clock).withMinute(0).withSecond(0).withNano(0).plusHours(1);
        BookingResponse b = bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), soon, soon.plusHours(2)));
        assertThat(b.expiresAt()).isBeforeOrEqualTo(soon.atZone(clock.getZone()).toInstant());
    }

    @Test
    void farmerCancelsAndProviderMarksWorkDone() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User farmer = data.seeker("Farmer");
        LocalDateTime six = data.day(2, 6);
        BookingResponse b = bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(4)));
        BookingResponse cancelled = bookingService.cancel(farmer.getId(), b.id());
        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(notifications(owner, "BOOKING_CANCELLED")).isEqualTo(1);

        BookingResponse b2 = bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(4)));
        bookingService.acceptItem(owner.getId(), b2.items().get(0).id());
        assertThatThrownBy(() -> bookingService.completeItem(owner.getId(), b2.items().get(0).id()))
                .hasMessageContaining("started");
        clock.advance(Duration.ofDays(3));
        assertThat(bookingService.completeItem(owner.getId(), b2.items().get(0).id()).status())
                .isEqualTo(BookingStatus.COMPLETED);
        assertThat(notifications(farmer, "BOOKING_COMPLETED")).isEqualTo(1);
    }

    @Test
    void onlyTheRightProviderCanAnswer() {
        User owner = data.provider("Owner");
        User stranger = data.provider("Stranger");
        Equipment tractor = data.tractor(owner);
        User farmer = data.seeker("Farmer");
        LocalDateTime six = data.day(2, 6);
        BookingResponse b = bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(4)));

        assertThatThrownBy(() -> bookingService.acceptItem(stranger.getId(), b.items().get(0).id()))
                .hasMessageContaining("someone else");
        assertThat(bookingService.busy(ResourceType.EQUIPMENT, tractor.getId(), six.toLocalDate(), 1))
                .hasSize(1);
        assertThat(List.of(b.items().get(0).status())).containsExactly(BookingItemStatus.PENDING);
    }

    private String status(Long bookingId) {
        return data.string("SELECT status FROM bookings WHERE id = ?", bookingId);
    }

    private int notifications(User user, String type) {
        return data.count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = ?", user.getId(), type);
    }
}
