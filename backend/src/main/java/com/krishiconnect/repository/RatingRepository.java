package com.krishiconnect.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.Equipment;

/** Keeps the rating totals on equipment and specialist rows in step with the reviews table. */
public interface RatingRepository extends Repository<Equipment, Long> {

    @Modifying
    @Query("update Equipment e set e.ratingSum = e.ratingSum + :rating, e.ratingCount = e.ratingCount + 1 where e.id = :id")
    int addEquipmentRating(@Param("id") Long equipmentId, @Param("rating") int rating);

    @Modifying
    @Query("""
            update SpecialistProfile p set p.ratingSum = p.ratingSum + :rating, p.ratingCount = p.ratingCount + 1
            where p.userId = :id
            """)
    int addSpecialistRating(@Param("id") Long specialistId, @Param("rating") int rating);
}
