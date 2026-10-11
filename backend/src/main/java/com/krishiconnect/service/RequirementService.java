package com.krishiconnect.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.dto.QuoteResponse;
import com.krishiconnect.dto.RequirementRequest;
import com.krishiconnect.dto.RequirementResponse;
import com.krishiconnect.entity.QuoteStatus;
import com.krishiconnect.entity.Requirement;
import com.krishiconnect.entity.RequirementStatus;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.BadRequestException;
import com.krishiconnect.exception.ConflictException;
import com.krishiconnect.exception.ForbiddenException;
import com.krishiconnect.exception.NotFoundException;
import com.krishiconnect.location.GeoUtils;
import com.krishiconnect.repository.GeoHit;
import com.krishiconnect.repository.QuoteRepository;
import com.krishiconnect.repository.RequirementRepository;
import com.krishiconnect.repository.UserRepository;
import com.krishiconnect.security.AuthUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public RequirementResponse create(Long seekerId, RequirementRequest request) {
        User seeker = userRepository.findById(seekerId).orElseThrow(() -> new NotFoundException("User not found"));
        if (seeker.getRole() != Role.SEEKER) {
            throw new ForbiddenException("Only farmers (seekers) can post requirements");
        }
        LocalDateTime start = request.startAt();
        LocalDateTime end = request.endAt();
        if (start.getMinute() != 0 || end.getMinute() != 0 || !end.isAfter(start)) {
            throw new BadRequestException("Choose whole hours, with the end after the start");
        }
        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("Start time must be in the future");
        }
        Requirement r = new Requirement();
        r.setSeeker(seeker);
        r.setTitle(request.title().trim());
        r.setDescription(request.description());
        r.setNeedType(request.needType());
        r.setCategory(request.needType() == ResourceType.EQUIPMENT ? request.category() : null);
        r.setSkill(request.needType() == ResourceType.SPECIALIST ? request.skill() : null);
        r.setCrop(request.crop());
        r.setAreaAcres(request.areaAcres());
        r.setStartAt(start);
        r.setEndAt(end);
        r.setVillage(request.village().trim());
        r.setDistrict(request.district());
        r.setLatitude(request.latitude());
        r.setLongitude(request.longitude());
        r.setBudget(request.budget());
        return RequirementResponse.from(requirementRepository.save(r), 0, null, null);
    }

    /** Open requirements. With a location they come nearest first (straight line is enough here). */
    @Transactional(readOnly = true)
    public PageResponse<RequirementResponse> board(Double lat, Double lng, Double radiusKm, ResourceType needType,
                                                   int page, int size) {
        if (lat == null || lng == null) {
            var result = requirementRepository.findBoard(RequirementStatus.OPEN, needType,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
            Map<Long, Long> counts = quoteCounts(result.getContent().stream().map(Requirement::getId).toList());
            return PageResponse.of(result, r -> RequirementResponse.from(r, counts.getOrDefault(r.getId(), 0L), null, null));
        }
        double radius = EquipmentService.clampRadius(radiusKm);
        var box = GeoUtils.boundingBox(lat, lng, radius);
        List<GeoHit> hits = requirementRepository.findNearby(lat, lng, box.minLat(), box.maxLat(), box.minLng(),
                box.maxLng(), needType == null ? null : needType.name(), radius, 300);
        List<Long> ids = hits.stream().map(GeoHit::getId).toList();
        Map<Long, Requirement> byId = ids.isEmpty() ? Map.of() : requirementRepository.findAllWithSeeker(ids).stream()
                .collect(Collectors.toMap(Requirement::getId, Function.identity()));
        Map<Long, Long> counts = quoteCounts(ids);
        List<RequirementResponse> all = hits.stream()
                .filter(h -> byId.containsKey(h.getId()))
                .map(h -> RequirementResponse.from(byId.get(h.getId()), counts.getOrDefault(h.getId(), 0L),
                        GeoUtils.round(h.getDistanceKm(), 1), null))
                .toList();
        return PageResponse.slice(all, page, size);
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> mine(Long seekerId) {
        List<Requirement> list = requirementRepository.findBySeekerIdOrderByCreatedAtDesc(seekerId);
        Map<Long, Long> counts = quoteCounts(list.stream().map(Requirement::getId).toList());
        return list.stream().map(r -> RequirementResponse.from(r, counts.getOrDefault(r.getId(), 0L), null, null)).toList();
    }

    /** The owner sees every quote; a provider sees only their own; anyone else just the post. */
    @Transactional(readOnly = true)
    public RequirementResponse get(Long id, AuthUser viewer) {
        Requirement r = requirementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Requirement " + id + " not found"));
        var quotes = quoteRepository.findByRequirement(id);
        long live = quotes.stream().filter(q -> q.getStatus() == QuoteStatus.PENDING || q.getStatus() == QuoteStatus.ACCEPTED).count();
        List<QuoteResponse> visible = null;
        if (viewer != null) {
            boolean owner = viewer.isAdmin() || r.getSeeker().getId().equals(viewer.getId());
            visible = quotes.stream()
                    .filter(q -> owner || q.getProvider().getId().equals(viewer.getId()))
                    .map(QuoteResponse::from)
                    .toList();
        }
        return RequirementResponse.from(r, live, null, visible);
    }

    @Transactional
    public RequirementResponse close(Long seekerId, Long id) {
        Requirement r = requirementRepository.lockById(id)
                .orElseThrow(() -> new NotFoundException("Requirement " + id + " not found"));
        if (!r.getSeeker().getId().equals(seekerId)) {
            throw new ForbiddenException("This is not your requirement");
        }
        if (r.getStatus() != RequirementStatus.OPEN) {
            throw new ConflictException("This requirement is already " + r.getStatus().name().toLowerCase());
        }
        r.setStatus(RequirementStatus.CLOSED);
        for (var q : quoteRepository.findByRequirement(id)) {
            if (q.getStatus() == QuoteStatus.PENDING) {
                q.setStatus(QuoteStatus.REJECTED);
                q.setRespondedAt(clock.instant());
                events.publishEvent(new QuoteEvent(q.getId(), QuoteEvent.Type.REJECTED));
            }
        }
        return RequirementResponse.from(r, 0, null, null);
    }

    private Map<Long, Long> quoteCounts(List<Long> ids) {
        Map<Long, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : quoteRepository.countByRequirementIds(ids)) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        return counts;
    }
}
