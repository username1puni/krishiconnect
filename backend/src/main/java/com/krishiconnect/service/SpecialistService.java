package com.krishiconnect.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.krishiconnect.dto.NearbyPageResponse;
import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.dto.SpecialistCard;
import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.SpecialistProfile;
import com.krishiconnect.exception.NotFoundException;
import com.krishiconnect.location.Distance;
import com.krishiconnect.location.DistanceService;
import com.krishiconnect.location.DistanceSource;
import com.krishiconnect.location.GeoUtils;
import com.krishiconnect.repository.GeoHit;
import com.krishiconnect.repository.SpecialistProfileRepository;
import com.krishiconnect.repository.SpecialistSearchRepository;
import com.krishiconnect.security.AuthUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpecialistService {

    private final SpecialistSearchRepository searchRepository;
    private final SpecialistProfileRepository profileRepository;
    private final DistanceService distanceService;

    public record SearchQuery(Double lat, Double lng, Double radiusKm, Skill skill, String sort) {
    }

    @Transactional(readOnly = true)
    public NearbyPageResponse<SpecialistCard> search(SearchQuery q, int page, int size) {
        if (q.lat() == null || q.lng() == null) {
            var result = searchRepository.browse(q.skill(),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ratingCount", "experienceYears")));
            return NearbyPageResponse.of(PageResponse.of(result, p -> SpecialistCard.from(p, null)), 0, 0);
        }
        double radius = EquipmentService.clampRadius(q.radiusKm());
        var box = GeoUtils.boundingBox(q.lat(), q.lng(), radius);
        List<GeoHit> hits = searchRepository.findNearby(q.lat(), q.lng(), box.minLat(), box.maxLat(), box.minLng(),
                box.maxLng(), q.skill() == null ? null : q.skill().name(), radius, 300);
        if (hits.isEmpty()) {
            return new NearbyPageResponse<>(List.of(), page, size, 0, 0, 0, 0);
        }
        Map<Long, SpecialistProfile> byId = profileRepository.findAllWithUser(hits.stream().map(GeoHit::getId).toList())
                .stream().collect(Collectors.toMap(SpecialistProfile::getUserId, Function.identity()));
        List<DistanceService.Target> targets = hits.stream()
                .filter(h -> byId.containsKey(h.getId()))
                .map(h -> {
                    var u = byId.get(h.getId()).getUser();
                    return new DistanceService.Target(u.getId(), u.getLatitude(), u.getLongitude(), h.getDistanceKm());
                })
                .toList();
        Map<Long, Distance> distances = distanceService.measure(q.lat(), q.lng(), targets);
        Comparator<SpecialistCard> byDistance = Comparator.comparing(SpecialistCard::distanceKm,
                Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<SpecialistCard> order = switch (q.sort() == null ? "distance" : q.sort()) {
            case "price" -> Comparator.comparing(SpecialistCard::ratePerDay, Comparator.nullsLast(BigDecimal::compareTo))
                    .thenComparing(byDistance);
            case "rating" -> Comparator.comparing(SpecialistCard::rating,
                    Comparator.nullsLast(Comparator.<Double>reverseOrder())).thenComparing(byDistance);
            default -> byDistance;
        };
        List<SpecialistCard> cards = targets.stream()
                .map(t -> SpecialistCard.from(byId.get(t.id()), distances.get(t.id())))
                .sorted(order)
                .toList();
        int road = (int) cards.stream().filter(c -> c.distanceSource() == DistanceSource.ROAD).count();
        return NearbyPageResponse.of(PageResponse.slice(cards, page, size), road, cards.size() - road);
    }

    @Transactional(readOnly = true)
    public SpecialistCard get(Long id, AuthUser viewer) {
        SpecialistProfile p = profileRepository.findAllWithUser(List.of(id)).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Specialist not found"));
        boolean self = viewer != null && (viewer.isAdmin() || viewer.getId().equals(id));
        if (!self && (!p.getUser().isVerified() || !p.getUser().isEnabled())) {
            throw new NotFoundException("Specialist not found");
        }
        return SpecialistCard.from(p, null);
    }
}
