package com.krishiconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.Requirement;
import com.krishiconnect.entity.RequirementStatus;
import com.krishiconnect.entity.ResourceType;

import jakarta.persistence.LockModeType;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Requirement r where r.id = :id")
    Optional<Requirement> lockById(@Param("id") Long id);

    @Query("""
            select r from Requirement r join fetch r.seeker
            where r.status = :status and (:needType is null or r.needType = :needType)
            """)
    Page<Requirement> findBoard(@Param("status") RequirementStatus status, @Param("needType") ResourceType needType,
                                Pageable pageable);

    @Query(value = """
            SELECT r.id AS id, 6371 * 2 * ASIN(SQRT(POWER(SIN(RADIANS(r.latitude - :lat) / 2), 2) + COS(RADIANS(:lat)) * COS(RADIANS(r.latitude)) * POWER(SIN(RADIANS(r.longitude - :lng) / 2), 2))) AS distanceKm
            FROM requirements r
            WHERE r.latitude BETWEEN :minLat AND :maxLat
              AND r.longitude BETWEEN :minLng AND :maxLng
              AND r.status = 'OPEN'
              AND (:needType IS NULL OR r.need_type = :needType)
            HAVING distanceKm <= :radiusKm
            ORDER BY distanceKm
            LIMIT :maxResults
            """, nativeQuery = true)
    List<GeoHit> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                            @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                            @Param("needType") String needType, @Param("radiusKm") double radiusKm,
                            @Param("maxResults") int maxResults);

    @Query("select r from Requirement r join fetch r.seeker where r.id in :ids")
    List<Requirement> findAllWithSeeker(@Param("ids") List<Long> ids);

    List<Requirement> findBySeekerIdOrderByCreatedAtDesc(Long seekerId);

    long countByStatus(RequirementStatus status);
}
