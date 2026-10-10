package com.krishiconnect.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.Skill;
import com.krishiconnect.entity.SpecialistProfile;

/** Read side for "find a specialist". Only verified, active and available people are listed. */
public interface SpecialistSearchRepository extends JpaRepository<SpecialistProfile, Long> {

    @Query(value = """
            SELECT u.id AS id, 6371 * 2 * ASIN(SQRT(POWER(SIN(RADIANS(u.latitude - :lat) / 2), 2) + COS(RADIANS(:lat)) * COS(RADIANS(u.latitude)) * POWER(SIN(RADIANS(u.longitude - :lng) / 2), 2))) AS distanceKm
            FROM users u
            JOIN specialist_profiles p ON p.user_id = u.id
            WHERE u.latitude BETWEEN :minLat AND :maxLat
              AND u.longitude BETWEEN :minLng AND :maxLng
              AND u.role = 'SPECIALIST' AND u.verified = TRUE AND u.enabled = TRUE
              AND p.available = TRUE
              AND (:skill IS NULL OR p.skill = :skill)
            HAVING distanceKm <= :radiusKm
            ORDER BY distanceKm
            LIMIT :maxResults
            """, nativeQuery = true)
    List<GeoHit> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                            @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                            @Param("minLng") double minLng, @Param("maxLng") double maxLng,
                            @Param("skill") String skill, @Param("radiusKm") double radiusKm,
                            @Param("maxResults") int maxResults);

    @Query(value = """
            select p from SpecialistProfile p join fetch p.user u
            where u.verified = true and u.enabled = true and p.available = true
              and (:skill is null or p.skill = :skill)
            """, countQuery = """
            select count(p) from SpecialistProfile p join p.user u
            where u.verified = true and u.enabled = true and p.available = true
              and (:skill is null or p.skill = :skill)
            """)
    Page<SpecialistProfile> browse(@Param("skill") Skill skill, Pageable pageable);
}
