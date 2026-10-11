package com.krishiconnect.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.krishiconnect.dto.RecommendationResponse;
import com.krishiconnect.entity.CropRule;
import com.krishiconnect.location.GeoUtils;
import com.krishiconnect.repository.CropRuleRepository;
import com.krishiconnect.repository.EquipmentRepository;
import com.krishiconnect.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Rule based seasonal suggestions. No machine learning on purpose: the farmer can see exactly why
 * something is suggested ("paddy harvest, November to December"), and the rules are a table an
 * agriculture officer could edit.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    static final double NEARBY_KM = 30;

    private final CropRuleRepository ruleRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<RecommendationResponse> forMonth(Integer month, String crop, Double lat, Double lng, Long userId) {
        int m = month == null ? LocalDate.now(clock).getMonthValue() : month;
        Set<String> myCrops = userId == null ? Set.of() : userRepository.findById(userId)
                .map(u -> u.getCrops() == null ? Set.<String>of()
                        : Arrays.stream(u.getCrops().split(",")).map(String::trim).collect(Collectors.toSet()))
                .orElse(Set.of());

        return ruleRepository.findAll().stream()
                .filter(r -> r.covers(m))
                .filter(r -> crop == null || crop.isBlank() || r.getCrop().equalsIgnoreCase(crop) || "ALL".equals(r.getCrop()))
                .sorted(Comparator.comparing((CropRule r) -> !myCrops.contains(r.getCrop()))
                        .thenComparing(r -> "ALL".equals(r.getCrop()))
                        .thenComparing(CropRule::getCrop)
                        .thenComparing(CropRule::getId))
                .map(r -> RecommendationResponse.from(r, myCrops.contains(r.getCrop()), nearby(r, lat, lng)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> crops() {
        return ruleRepository.findCrops();
    }

    private Long nearby(CropRule rule, Double lat, Double lng) {
        if (lat == null || lng == null || rule.getEquipmentCategory() == null) {
            return null;
        }
        var box = GeoUtils.boundingBox(lat, lng, NEARBY_KM);
        return equipmentRepository.countNearby(lat, lng, box.minLat(), box.maxLat(), box.minLng(), box.maxLng(),
                rule.getEquipmentCategory().name(), NEARBY_KM);
    }
}
