package com.krishiconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findTop20ByNameStartingWithIgnoreCaseOrderByName(String prefix);

    List<Place> findAllByOrderByDistrictAscNameAsc();

    @Query(value = """
            SELECT * FROM places p
            ORDER BY POWER(p.latitude - :lat, 2) + POWER((p.longitude - :lng) * COS(RADIANS(:lat)), 2)
            LIMIT 1
            """, nativeQuery = true)
    Optional<Place> findNearest(@Param("lat") double lat, @Param("lng") double lng);
}
