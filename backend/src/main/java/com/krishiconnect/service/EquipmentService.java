package com.krishiconnect.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.krishiconnect.dto.EquipmentCard;
import com.krishiconnect.dto.EquipmentDetail;
import com.krishiconnect.dto.EquipmentRequest;
import com.krishiconnect.dto.NearbyPageResponse;
import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.EquipmentPhoto;
import com.krishiconnect.entity.EquipmentStatus;
import com.krishiconnect.entity.ModerationStatus;
import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.User;
import com.krishiconnect.exception.BadRequestException;
import com.krishiconnect.exception.ForbiddenException;
import com.krishiconnect.exception.NotFoundException;
import com.krishiconnect.location.Distance;
import com.krishiconnect.location.DistanceService;
import com.krishiconnect.location.DistanceSource;
import com.krishiconnect.location.GeoUtils;
import com.krishiconnect.repository.EquipmentPhotoRepository;
import com.krishiconnect.repository.EquipmentRepository;
import com.krishiconnect.repository.GeoHit;
import com.krishiconnect.repository.UserRepository;
import com.krishiconnect.security.AuthUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    static final int MAX_PHOTOS = 4;
    private static final int MAX_CANDIDATES = 300;
    private static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final EquipmentRepository equipmentRepository;
    private final EquipmentPhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final DistanceService distanceService;

    public record SearchQuery(Double lat, Double lng, Double radiusKm, EquipmentCategory category, String sort) {
    }

    @Transactional(readOnly = true)
    public NearbyPageResponse<EquipmentCard> search(SearchQuery q, int page, int size) {
        if (q.lat() == null || q.lng() == null) {
            Sort sort = "price".equals(q.sort())
                    ? Sort.by(Sort.Order.asc("pricePerHour").nullsLast(), Sort.Order.desc("createdAt"))
                    : Sort.by(Sort.Direction.DESC, "createdAt");
            var result = equipmentRepository.browse(q.category(), PageRequest.of(page, size, sort));
            Map<Long, Long> covers = coverPhotos(result.getContent().stream().map(Equipment::getId).toList());
            return NearbyPageResponse.of(PageResponse.of(result, e -> EquipmentCard.from(e, covers.get(e.getId()), null)), 0, 0);
        }

        double radius = clampRadius(q.radiusKm());
        var box = GeoUtils.boundingBox(q.lat(), q.lng(), radius);
        List<GeoHit> hits = equipmentRepository.findNearby(q.lat(), q.lng(), box.minLat(), box.maxLat(),
                box.minLng(), box.maxLng(), q.category() == null ? null : q.category().name(), radius, MAX_CANDIDATES);
        if (hits.isEmpty()) {
            return new NearbyPageResponse<>(List.of(), page, size, 0, 0, 0, 0);
        }
        Map<Long, Equipment> byId = equipmentRepository.findAllWithOwner(hits.stream().map(GeoHit::getId).toList())
                .stream().collect(Collectors.toMap(Equipment::getId, Function.identity()));
        List<DistanceService.Target> targets = hits.stream()
                .filter(h -> byId.containsKey(h.getId()))
                .map(h -> {
                    Equipment e = byId.get(h.getId());
                    return new DistanceService.Target(e.getId(), e.getLatitude(), e.getLongitude(), h.getDistanceKm());
                })
                .toList();
        Map<Long, Distance> distances = distanceService.measure(q.lat(), q.lng(), targets);
        Map<Long, Long> covers = coverPhotos(byId.keySet());

        List<EquipmentCard> cards = targets.stream()
                .map(t -> EquipmentCard.from(byId.get(t.id()), covers.get(t.id()), distances.get(t.id())))
                .sorted(comparator(q.sort()))
                .toList();
        int road = (int) cards.stream().filter(c -> c.distanceSource() == DistanceSource.ROAD).count();
        return NearbyPageResponse.of(PageResponse.slice(cards, page, size), road, cards.size() - road);
    }

    @Transactional(readOnly = true)
    public EquipmentDetail get(Long id, AuthUser viewer) {
        Equipment e = equipmentRepository.findWithOwner(id).orElseThrow(() -> notFound(id));
        boolean ownerOrAdmin = viewer != null && (viewer.isAdmin() || viewer.getId().equals(e.getOwner().getId()));
        if (!e.isBookable() && !ownerOrAdmin) {
            throw notFound(id);
        }
        return EquipmentDetail.from(e, photoRepository.findIdsByEquipmentId(id));
    }

    @Transactional(readOnly = true)
    public List<EquipmentDetail> mine(Long ownerId) {
        return equipmentRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(e -> EquipmentDetail.from(e, photoRepository.findIdsByEquipmentId(e.getId())))
                .toList();
    }

    @Transactional
    public EquipmentDetail create(Long ownerId, EquipmentRequest request) {
        User owner = userRepository.findById(ownerId).orElseThrow(() -> new NotFoundException("User not found"));
        if (owner.getRole() != Role.PROVIDER) {
            throw new ForbiddenException("Only equipment providers can add listings");
        }
        Equipment e = new Equipment();
        e.setOwner(owner);
        apply(e, request);
        // listings from verified providers go live straight away, the rest wait for an admin
        e.setModerationStatus(owner.isVerified() ? ModerationStatus.APPROVED : ModerationStatus.PENDING);
        return EquipmentDetail.from(equipmentRepository.save(e), List.of());
    }

    @Transactional
    public EquipmentDetail update(Long ownerId, Long id, EquipmentRequest request) {
        Equipment e = owned(ownerId, id);
        apply(e, request);
        if (e.getModerationStatus() == ModerationStatus.REJECTED) {
            // edited after a rejection: send it back for another look
            e.setModerationStatus(ModerationStatus.PENDING);
        }
        return EquipmentDetail.from(e, photoRepository.findIdsByEquipmentId(id));
    }

    @Transactional
    public EquipmentDetail setStatus(Long ownerId, Long id, EquipmentStatus status) {
        Equipment e = owned(ownerId, id);
        e.setStatus(status);
        return EquipmentDetail.from(e, photoRepository.findIdsByEquipmentId(id));
    }

    @Transactional
    public Long addPhoto(Long ownerId, Long id, MultipartFile file) {
        Equipment e = owned(ownerId, id);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose a photo to upload");
        }
        if (!PHOTO_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only JPG, PNG or WEBP photos are allowed");
        }
        long count = photoRepository.countByEquipmentId(id);
        if (count >= MAX_PHOTOS) {
            throw new BadRequestException("A listing can have at most " + MAX_PHOTOS + " photos");
        }
        try {
            return photoRepository.save(new EquipmentPhoto(e, file.getContentType(), file.getBytes(), (int) count)).getId();
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the uploaded photo");
        }
    }

    @Transactional
    public void deletePhoto(Long ownerId, Long id, Long photoId) {
        owned(ownerId, id);
        if (!photoRepository.findIdsByEquipmentId(id).contains(photoId)) {
            throw new NotFoundException("Photo not found");
        }
        photoRepository.deleteById(photoId);
    }

    @Transactional(readOnly = true)
    public EquipmentPhoto photo(Long photoId) {
        return photoRepository.findById(photoId).orElseThrow(() -> new NotFoundException("Photo not found"));
    }

    private Equipment owned(Long ownerId, Long id) {
        Equipment e = equipmentRepository.findWithOwner(id).orElseThrow(() -> notFound(id));
        if (!e.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("This listing belongs to someone else");
        }
        return e;
    }

    private static void apply(Equipment e, EquipmentRequest r) {
        if (r.pricePerHour() == null && r.pricePerDay() == null && r.pricePerAcre() == null) {
            throw new BadRequestException("Give at least one price: per hour, per day or per acre");
        }
        e.setTitle(r.title().trim());
        e.setCategory(r.category());
        e.setBrand(r.brand());
        e.setModel(r.model());
        e.setYearOfMake(r.yearOfMake());
        e.setHorsePower(r.horsePower());
        e.setDescription(r.description());
        e.setPricePerHour(r.pricePerHour());
        e.setPricePerDay(r.pricePerDay());
        e.setPricePerAcre(r.pricePerAcre());
        e.setWithOperator(r.withOperator());
        e.setVillage(r.village().trim());
        e.setDistrict(r.district());
        e.setLatitude(r.latitude());
        e.setLongitude(r.longitude());
    }

    private Map<Long, Long> coverPhotos(Collection<Long> ids) {
        Map<Long, Long> covers = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : photoRepository.findCoverPhotoIds(ids)) {
                covers.put((Long) row[0], (Long) row[1]);
            }
        }
        return covers;
    }

    static double clampRadius(Double radiusKm) {
        if (radiusKm == null) {
            return 50;
        }
        return Math.max(1, Math.min(radiusKm, 300));
    }

    private static Comparator<EquipmentCard> comparator(String sort) {
        Comparator<EquipmentCard> byDistance = Comparator.comparing(EquipmentCard::distanceKm,
                Comparator.nullsLast(Comparator.naturalOrder()));
        if ("price".equals(sort)) {
            return Comparator.comparing(EquipmentCard::pricePerHour, Comparator.nullsLast(BigDecimal::compareTo))
                    .thenComparing(byDistance);
        }
        if ("rating".equals(sort)) {
            return Comparator.comparing(EquipmentCard::rating, Comparator.nullsLast(Comparator.<Double>reverseOrder()))
                    .thenComparing(byDistance);
        }
        return byDistance;
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("Equipment " + id + " not found");
    }
}
