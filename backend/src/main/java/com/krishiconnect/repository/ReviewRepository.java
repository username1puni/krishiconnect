package com.krishiconnect.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingItemId(Long bookingItemId);

    @Query("select r from Review r join fetch r.reviewer where r.equipmentId = :id")
    Page<Review> findByEquipment(@Param("id") Long equipmentId, Pageable pageable);

    @Query("select r from Review r join fetch r.reviewer where r.specialistId = :id")
    Page<Review> findBySpecialist(@Param("id") Long specialistId, Pageable pageable);

    @Query("select r.bookingItemId from Review r where r.reviewer.id = :reviewerId")
    List<Long> findReviewedItemIds(@Param("reviewerId") Long reviewerId);
}
