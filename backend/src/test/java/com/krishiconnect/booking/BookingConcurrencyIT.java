package com.krishiconnect.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.krishiconnect.dto.BookingItemRequest;
import com.krishiconnect.dto.CreateBookingRequest;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.PriceUnit;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.exception.ResourceUnavailableException;
import com.krishiconnect.service.BookingService;
import com.krishiconnect.support.IntegrationTest;

/**
 * The main guarantee of the app: the same tractor or person can never be booked twice for
 * overlapping time, even when many farmers press "Book" at the same moment.
 */
class BookingConcurrencyIT extends IntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void twentyFarmersBookingOverlappingTimesOnOneTractorOnlyOneWins() throws Exception {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        List<User> farmers = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            farmers.add(data.seeker("Farmer " + i));
        }
        LocalDateTime base = data.day(3, 6);

        // every request overlaps every other one: all of them cover 9 AM to 11 AM
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            User farmer = farmers.get(i);
            LocalDateTime start = base.plusHours(i % 4);
            LocalDateTime end = base.plusHours(5 + i % 3);
            tasks.add(() -> bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), start, end)).id());
        }
        Result result = runTogether(tasks);

        assertThat(result.successes).hasSize(1);
        assertThat(result.conflicts.get()).isEqualTo(19);
        assertThat(result.other).isEmpty();
        assertThat(data.count("SELECT COUNT(*) FROM booking_items WHERE equipment_id = ? AND status = 'PENDING'",
                tractor.getId())).isEqualTo(1);
        // and the winner holds exactly one slot row per hour of its own booking
        int hours = data.count("SELECT TIMESTAMPDIFF(HOUR, start_at, end_at) FROM booking_items WHERE equipment_id = ?",
                tractor.getId());
        assertThat(data.count("SELECT COUNT(*) FROM availability_slots WHERE resource_id = ? AND resource_type = 'EQUIPMENT'",
                tractor.getId())).isEqualTo(hours);
    }

    @Test
    void backToBackBookingsDoNotCountAsOverlap() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User a = data.seeker("A");
        User b = data.seeker("B");
        LocalDateTime six = data.day(2, 6);

        bookingService.create(a.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(4)));
        // starts exactly when the first one ends
        bookingService.create(b.getId(), data.equipmentBooking(tractor.getId(), six.plusHours(4), six.plusHours(8)));

        assertThatThrownBy(() -> bookingService.create(b.getId(),
                data.equipmentBooking(tractor.getId(), six.plusHours(3), six.plusHours(5))))
                .isInstanceOf(ResourceUnavailableException.class)
                .hasMessageContaining("Swaraj 744 FE");
    }

    @Test
    void bundleWithABusyDriverBooksNothing() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User driver = data.specialist("Driver", Skill.TRACTOR_DRIVER);
        User someone = data.seeker("Someone");
        User farmer = data.seeker("Farmer");
        LocalDateTime six = data.day(2, 6);

        // the driver is already taken in the morning
        bookingService.create(someone.getId(), new CreateBookingRequest(
                List.of(new BookingItemRequest(ResourceType.SPECIALIST, driver.getId(), PriceUnit.DAY, null)),
                six, six.plusHours(4), null));

        assertThatThrownBy(() -> bookingService.create(farmer.getId(),
                data.bundle(tractor.getId(), driver.getId(), six.plusHours(2), six.plusHours(8))))
                .isInstanceOf(ResourceUnavailableException.class)
                .hasMessageContaining("Driver");

        // the tractor part was rolled back with it
        assertThat(data.count("SELECT COUNT(*) FROM booking_items WHERE equipment_id = ?", tractor.getId())).isZero();
        assertThat(data.count("SELECT COUNT(*) FROM availability_slots WHERE resource_type = 'EQUIPMENT'")).isZero();
        assertThat(data.count("SELECT COUNT(*) FROM bookings WHERE seeker_id = ?", farmer.getId())).isZero();
    }

    @Test
    void bundlesAndSingleBookingsRacingForTheSameDriverNeverDeadlock() throws Exception {
        User owner = data.provider("Owner");
        Equipment tractor1 = data.tractor(owner);
        Equipment tractor2 = data.tractor(owner);
        User driver = data.specialist("Driver", Skill.TRACTOR_DRIVER);
        LocalDateTime six = data.day(4, 6);

        // three kinds of request at once, listing the items in different orders:
        // [tractor1 + driver], [driver + tractor2], [driver alone]. All need the driver, so one wins.
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            User farmer = data.seeker("Farmer " + i);
            List<BookingItemRequest> items = switch (i % 3) {
                case 0 -> List.of(item(ResourceType.EQUIPMENT, tractor1.getId()), item(ResourceType.SPECIALIST, driver.getId()));
                case 1 -> List.of(item(ResourceType.SPECIALIST, driver.getId()), item(ResourceType.EQUIPMENT, tractor2.getId()));
                default -> List.of(item(ResourceType.SPECIALIST, driver.getId()));
            };
            CreateBookingRequest request = new CreateBookingRequest(items, six, six.plusHours(6), null);
            tasks.add(() -> bookingService.create(farmer.getId(), request).id());
        }
        Result result = runTogether(tasks);

        assertThat(result.successes).hasSize(1);
        assertThat(result.conflicts.get()).isEqualTo(17);
        assertThat(result.other).as("no deadlocks or other errors").isEmpty();
        assertThat(data.count("SELECT COUNT(*) FROM booking_items WHERE specialist_id = ?", driver.getId())).isEqualTo(1);
    }

    @Test
    void reverseOrderBundlesOnTwoMachinesNeverDeadlock() throws Exception {
        User owner = data.provider("Owner");
        Equipment a = data.tractor(owner);
        Equipment b = data.tractor(owner);
        LocalDateTime six = data.day(5, 6);

        // half the requests ask for [a, b], half for [b, a]; locks are always taken in id order
        List<Callable<Long>> tasks = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            User farmer = data.seeker("Farmer " + i);
            List<BookingItemRequest> items = i % 2 == 0
                    ? List.of(item(ResourceType.EQUIPMENT, a.getId()), item(ResourceType.EQUIPMENT, b.getId()))
                    : List.of(item(ResourceType.EQUIPMENT, b.getId()), item(ResourceType.EQUIPMENT, a.getId()));
            CreateBookingRequest request = new CreateBookingRequest(items, six, six.plusHours(3), null);
            tasks.add(() -> bookingService.create(farmer.getId(), request).id());
        }
        Result result = runTogether(tasks);

        assertThat(result.successes).hasSize(1);
        assertThat(result.conflicts.get()).isEqualTo(15);
        assertThat(result.other).isEmpty();
    }

    @Test
    void uniqueKeyOnSlotsBlocksADoubleBookingEvenWithoutTheLock() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User farmer = data.seeker("Farmer");
        LocalDateTime six = data.day(2, 6);
        bookingService.create(farmer.getId(), data.equipmentBooking(tractor.getId(), six, six.plusHours(2)));
        Long itemId = jdbc.queryForObject("SELECT id FROM booking_items WHERE equipment_id = ?", Long.class, tractor.getId());

        // simulate a buggy code path that skipped the lock and the overlap check
        assertThatThrownBy(() -> jdbc.update("INSERT INTO availability_slots (resource_type, resource_id, slot_start, "
                + "booking_item_id) VALUES ('EQUIPMENT', ?, ?, ?)", tractor.getId(), six.plusHours(1), itemId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cannotBookInThePastOrOffTheHour() {
        User owner = data.provider("Owner");
        Equipment tractor = data.tractor(owner);
        User farmer = data.seeker("Farmer");
        LocalDateTime yesterday = data.day(-1, 6);

        assertThatThrownBy(() -> bookingService.create(farmer.getId(),
                data.equipmentBooking(tractor.getId(), yesterday, yesterday.plusHours(2))))
                .hasMessageContaining("future");
        assertThatThrownBy(() -> bookingService.create(farmer.getId(),
                data.equipmentBooking(tractor.getId(), data.day(2, 6).plusMinutes(30), data.day(2, 9))))
                .hasMessageContaining("on the hour");
        assertThatThrownBy(() -> bookingService.create(owner.getId(),
                data.equipmentBooking(tractor.getId(), data.day(2, 6), data.day(2, 9))))
                .hasMessageContaining("own equipment");
    }

    private static BookingItemRequest item(ResourceType type, Long id) {
        return new BookingItemRequest(type, id, type == ResourceType.EQUIPMENT ? PriceUnit.HOUR : PriceUnit.DAY, null);
    }

    private record Result(List<Long> successes, AtomicInteger conflicts, List<Throwable> other) {
    }

    /** Starts all tasks at the same instant and sorts the outcomes. */
    private static Result runTogether(List<Callable<Long>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        for (Callable<Long> task : tasks) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            }));
        }
        ready.await(10, TimeUnit.SECONDS);
        go.countDown();

        List<Long> successes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger conflicts = new AtomicInteger();
        List<Throwable> other = new ArrayList<>();
        for (Future<Long> f : futures) {
            try {
                successes.add(f.get(60, TimeUnit.SECONDS));
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ConflictException) {
                    conflicts.incrementAndGet();
                } else {
                    other.add(e.getCause());
                }
            }
        }
        pool.shutdown();
        return new Result(successes, conflicts, other);
    }
}
