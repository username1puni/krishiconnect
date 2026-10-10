package com.krishiconnect.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.ModerationStatus;

import jakarta.persistence.LockModeType;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    /**
     * Straight line ("haversine") distance in SQL. The BETWEEN on latitude/longitude is the cheap
     * bounding box that lets MySQL use idx_equipment_geo; the formula then only runs on the rows
     * inside the box.
     */
    @Query(value = """
            SELECT e.id AS id, 6371 * 2 * ASIN(SQRT(POWER(SIN(RADIANS(e.latitude - :lat) / 2), 2) + COS(RADIANS(:lat)) * COS(RADIANS(e.latitude)) * POWER(SIN(RADIANS(e.longitude - :lng) / 2), 2))) AS distanceKm
            FROM equipment e
            WHERE e.latitude BETWEEN :minLat AND :maxLat
              AND e.longitude BETWEEN :minLng AND :maxLng
              AND e.status = 'ACTIVE' AND e.moderation_status = 'APPROVED'
              AND (:category IS NULL OR e.category = :category)
            HAVING distanceKm <= :radiusKm
            ORDER BY distanceKm
            LIMIT :maxResults
            """, nativeQuery = true)
    List<GeoHit> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                            @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                            @Param("category") String category, @Param("radiusKm") double radiusKm,
                            @Param("maxResults") int maxResults);

    @Query("""
            select e from Equipment e join fetch e.owner
            where e.status = com.krishiconnect.entity.EquipmentStatus.ACTIVE
              and e.moderationStatus = com.krishiconnect.entity.ModerationStatus.APPROVED
              and (:category is null or e.category = :category)
            """)
    Page<Equipment> browse(@Param("category") EquipmentCategory category, Pageable pageable);

    @Query("select e from Equipment e join fetch e.owner where e.id in :ids")
    List<Equipment> findAllWithOwner(@Param("ids") Collection<Long> ids);

    @Query("select e from Equipment e join fetch e.owner where e.id = :id")
    Optional<Equipment> findWithOwner(@Param("id") Long id);

    List<Equipment> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /**
     * SELECT ... FOR UPDATE on equipment rows only, in id order. No join here on purpose: joining
     * users would lock those rows too, and in an order MySQL picks, which is how deadlocks start.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipment e where e.id in :ids order by e.id")
    List<Equipment> lockAllByIds(@Param("ids") Collection<Long> ids);

    @Query("select e from Equipment e join fetch e.owner where e.moderationStatus = :status")
    Page<Equipment> findByModeration(@Param("status") ModerationStatus status, Pageable pageable);

    @Query("select e from Equipment e join fetch e.owner")
    Page<Equipment> findAllWithOwner(Pageable pageable);

    long countByModerationStatus(ModerationStatus status);

    @Query(value = """
            SELECT COUNT(*) FROM equipment e
            WHERE e.latitude BETWEEN :minLat AND :maxLat
              AND e.longitude BETWEEN :minLng AND :maxLng
              AND e.status = 'ACTIVE' AND e.moderation_status = 'APPROVED'
              AND e.category = :category
              AND 6371 * 2 * ASIN(SQRT(POWER(SIN(RADIANS(e.latitude - :lat) / 2), 2) + COS(RADIANS(:lat)) * COS(RADIANS(e.latitude)) * POWER(SIN(RADIANS(e.longitude - :lng) / 2), 2))) <= :radiusKm
            """, nativeQuery = true)
    long countNearby(@Param("lat") double lat, @Param("lng") double lng,
                     @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                     @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                     @Param("category") String category, @Param("radiusKm") double radiusKm);
}
