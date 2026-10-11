package com.krishiconnect.requirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishiconnect.dto.QuoteRequest;
import com.krishiconnect.dto.QuoteResponse;
import com.krishiconnect.dto.RequirementRequest;
import com.krishiconnect.dto.RequirementResponse;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.QuoteStatus;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.exception.ForbiddenException;
import com.krishiconnect.service.BookingService;
import com.krishiconnect.service.QuoteService;
import com.krishiconnect.service.RequirementService;
import com.krishiconnect.support.IntegrationTest;

class QuoteFlowIT extends IntegrationTest {

    @Autowired
    private RequirementService requirementService;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private BookingService bookingService;

    @Test
    void farmerAcceptsOneQuoteAndTheRestAreTurnedDown() {
        User farmer = data.seeker("Ramesh");
        User p1 = data.provider("Provider One");
        User p2 = data.provider("Provider Two");
        Equipment h1 = data.equipment(p1, "Kartar 4000", EquipmentCategory.COMBINE_HARVESTER, 12.52, 76.89);
        Equipment h2 = data.equipment(p2, "Preet 987", EquipmentCategory.COMBINE_HARVESTER, 12.42, 76.69);
        RequirementResponse r = post(farmer, data.day(4, 7));

        QuoteResponse q1 = quoteService.submit(p1.getId(), r.id(), new QuoteRequest(h1.getId(), new BigDecimal("9000"), "Can come early"));
        QuoteResponse q2 = quoteService.submit(p2.getId(), r.id(), new QuoteRequest(h2.getId(), new BigDecimal("10500"), null));
        assertThat(data.count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'QUOTE_RECEIVED'", farmer.getId()))
                .isEqualTo(2);

        QuoteResponse accepted = quoteService.accept(farmer.getId(), r.id(), q1.id());

        assertThat(accepted.status()).isEqualTo(QuoteStatus.ACCEPTED);
        assertThat(accepted.bookingId()).isNotNull();
        assertThat(data.string("SELECT status FROM bookings WHERE id = ?", accepted.bookingId())).isEqualTo("CONFIRMED");
        assertThat(data.string("SELECT total_amount FROM bookings WHERE id = ?", accepted.bookingId())).isEqualTo("9000.00");
        assertThat(data.string("SELECT status FROM quotes WHERE id = ?", q2.id())).isEqualTo("REJECTED");
        assertThat(data.string("SELECT status FROM requirements WHERE id = ?", r.id())).isEqualTo("ASSIGNED");
        assertThat(data.count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'QUOTE_ACCEPTED'", p1.getId())).isEqualTo(1);
        assertThat(data.count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'QUOTE_REJECTED'", p2.getId())).isEqualTo(1);
        // the harvester's calendar is blocked for those hours
        assertThat(data.count("SELECT COUNT(*) FROM availability_slots WHERE resource_id = ?", h1.getId())).isEqualTo(5);
    }

    @Test
    void twoQuotesAcceptedAtTheSameMomentOnlyOneWins() throws Exception {
        User farmer = data.seeker("Ramesh");
        User p1 = data.provider("One");
        User p2 = data.provider("Two");
        Equipment h1 = data.equipment(p1, "H1", EquipmentCategory.COMBINE_HARVESTER, 12.52, 76.89);
        Equipment h2 = data.equipment(p2, "H2", EquipmentCategory.COMBINE_HARVESTER, 12.52, 76.89);
        RequirementResponse r = post(farmer, data.day(4, 7));
        Long q1 = quoteService.submit(p1.getId(), r.id(), new QuoteRequest(h1.getId(), new BigDecimal("9000"), null)).id();
        Long q2 = quoteService.submit(p2.getId(), r.id(), new QuoteRequest(h2.getId(), new BigDecimal("9500"), null)).id();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<QuoteResponse>> futures = new ArrayList<>();
        for (Long q : List.of(q1, q2)) {
            futures.add(pool.submit(() -> {
                go.await();
                return quoteService.accept(farmer.getId(), r.id(), q);
            }));
        }
        go.countDown();
        int ok = 0;
        int conflict = 0;
        for (Future<QuoteResponse> f : futures) {
            try {
                f.get(30, TimeUnit.SECONDS);
                ok++;
            } catch (Exception e) {
                assertThat(e.getCause()).isInstanceOf(ConflictException.class);
                conflict++;
            }
        }
        pool.shutdown();
        assertThat(ok).isEqualTo(1);
        assertThat(conflict).isEqualTo(1);
        assertThat(data.count("SELECT COUNT(*) FROM bookings")).isEqualTo(1);
        assertThat(data.count("SELECT COUNT(*) FROM quotes WHERE status = 'ACCEPTED'")).isEqualTo(1);
    }

    @Test
    void acceptingAQuoteForAMachineBookedSinceThenChangesNothing() {
        User farmer = data.seeker("Ramesh");
        User other = data.seeker("Other");
        User p1 = data.provider("One");
        Equipment h1 = data.equipment(p1, "Kartar 4000", EquipmentCategory.COMBINE_HARVESTER, 12.52, 76.89);
        LocalDateTime start = data.day(4, 7);
        RequirementResponse r = post(farmer, start);
        Long q1 = quoteService.submit(p1.getId(), r.id(), new QuoteRequest(h1.getId(), new BigDecimal("9000"), null)).id();

        // someone booked the same harvester directly in the meantime
        bookingService.create(other.getId(), data.equipmentBooking(h1.getId(), start.plusHours(1), start.plusHours(3)));

        assertThatThrownBy(() -> quoteService.accept(farmer.getId(), r.id(), q1)).isInstanceOf(ConflictException.class);
        assertThat(data.string("SELECT status FROM quotes WHERE id = ?", q1)).isEqualTo("PENDING");
        assertThat(data.string("SELECT status FROM requirements WHERE id = ?", r.id())).isEqualTo("OPEN");
    }

    @Test
    void onlyTheRightKindOfProviderCanQuote() {
        User farmer = data.seeker("Ramesh");
        User driver = data.specialist("Driver", Skill.TRACTOR_DRIVER);
        User p1 = data.provider("One");
        RequirementResponse r = post(farmer, data.day(4, 7));

        assertThatThrownBy(() -> quoteService.submit(driver.getId(), r.id(), new QuoteRequest(null, BigDecimal.TEN, null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> quoteService.submit(p1.getId(), r.id(), new QuoteRequest(null, BigDecimal.TEN, null)))
                .hasMessageContaining("machines");
    }

    private RequirementResponse post(User farmer, LocalDateTime start) {
        return requirementService.create(farmer.getId(), new RequirementRequest("Harvester for 4 acres paddy",
                "Dry field", ResourceType.EQUIPMENT, EquipmentCategory.COMBINE_HARVESTER, null, "PADDY",
                new BigDecimal("4"), start, start.plusHours(5), "Keragodu", "Mandya", 12.61, 76.86, new BigDecimal("9500")));
    }
}
