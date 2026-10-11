package com.krishiconnect.location;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishiconnect.dto.EquipmentCard;
import com.krishiconnect.dto.NearbyPageResponse;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.User;
import com.krishiconnect.service.EquipmentService;
import com.krishiconnect.support.IntegrationTest;

class NearbySearchIT extends IntegrationTest {

    // Keragodu, a village north of Mandya
    private static final double LAT = 12.6146;
    private static final double LNG = 76.8580;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private DistanceService distanceService;

    private Equipment acrossTheRiver;
    private Equipment byRoad;
    private Equipment farAway;

    @BeforeEach
    void listings() {
        distanceService.setEnabled(true);
        distanceService.clearCache();
        User owner = data.provider("Owner");
        // closer in a straight line, but the road goes around: 9 km straight, 21 km by road
        acrossTheRiver = data.equipment(owner, "Across the river", EquipmentCategory.TRACTOR, 12.5450, 76.8200);
        // a bit further in a straight line, but on the highway: 11 km straight, 13 km by road
        byRoad = data.equipment(owner, "On the highway", EquipmentCategory.TRACTOR, 12.5230, 76.8970);
        // Chitradurga, ~190 km away: outside the box for a 50 km search
        farAway = data.equipment(owner, "Chitradurga tractor", EquipmentCategory.TRACTOR, 14.2251, 76.3980);
    }

    @AfterEach
    void routingOff() {
        distanceService.setEnabled(false);
    }

    @Test
    void sortsByRoadDistanceWhenTheRoutingApiAnswers() {
        Map<Long, Double> roadKm = Map.of(acrossTheRiver.getId(), 21.0, byRoad.getId(), 13.0);
        routing.answerWith(p -> {
            Long id = Math.abs(p.lat() - 12.5450) < 1e-6 ? acrossTheRiver.getId() : byRoad.getId();
            return new RouteLeg(roadKm.get(id) * 1000, roadKm.get(id) * 90);
        });

        NearbyPageResponse<EquipmentCard> page = search(50);

        assertThat(page.content()).extracting(EquipmentCard::title).containsExactly("On the highway", "Across the river");
        assertThat(page.content()).extracting(EquipmentCard::distanceSource).containsOnly(DistanceSource.ROAD);
        assertThat(page.content().get(0).distanceKm()).isEqualTo(13.0);
        assertThat(page.content().get(0).durationMinutes()).isEqualTo(20);
        assertThat(page.roadDistances()).isEqualTo(2);
        assertThat(page.content()).extracting(EquipmentCard::id).doesNotContain(farAway.getId());
    }

    @Test
    void fallsBackToStraightLineWhenTheApiIsDownAndSkipsItForAWhile() {
        routing.goDown();

        NearbyPageResponse<EquipmentCard> page = search(50);

        assertThat(page.content()).extracting(EquipmentCard::title).containsExactly("Across the river", "On the highway");
        assertThat(page.content()).extracting(EquipmentCard::distanceSource).containsOnly(DistanceSource.STRAIGHT_LINE);
        assertThat(page.straightLineDistances()).isEqualTo(2);
        assertThat(routing.calls.get()).isEqualTo(1);

        // during the cool-down the API is not called at all, so searches stay fast
        search(50);
        assertThat(routing.calls.get()).isEqualTo(1);
        assertThat(distanceService.status().coolingDown()).isTrue();
    }

    @Test
    void cachesRoadDistancesForTheSameVillage() {
        routing.answerWith(p -> new RouteLeg(15_000, 1_200));
        search(50);
        // a farmer a few metres away in the same village reuses the cached answers
        equipmentService.search(new EquipmentService.SearchQuery(LAT + 0.0001, LNG, 50.0, null, "distance"), 0, 10);
        assertThat(routing.calls.get()).isEqualTo(1);
        assertThat(distanceService.status().cacheHits()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void radiusAndCategoryFilterInSql() {
        assertThat(search(5).content()).isEmpty();
        assertThat(search(300).totalElements()).isEqualTo(3);
        var harvesters = equipmentService.search(
                new EquipmentService.SearchQuery(LAT, LNG, 300.0, EquipmentCategory.COMBINE_HARVESTER, "distance"), 0, 10);
        assertThat(harvesters.totalElements()).isZero();
    }

    @Test
    void pagesThroughSortedResults() {
        var first = equipmentService.search(new EquipmentService.SearchQuery(LAT, LNG, 300.0, null, "distance"), 0, 2);
        var second = equipmentService.search(new EquipmentService.SearchQuery(LAT, LNG, 300.0, null, "distance"), 1, 2);
        assertThat(first.content()).hasSize(2);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(second.content()).extracting(EquipmentCard::title).containsExactly("Chitradurga tractor");
    }

    private NearbyPageResponse<EquipmentCard> search(double radiusKm) {
        return equipmentService.search(new EquipmentService.SearchQuery(LAT, LNG, radiusKm, null, "distance"), 0, 10);
    }
}
