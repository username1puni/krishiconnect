package com.krishiconnect.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.Booking;
import com.krishiconnect.entity.BookingStatus;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Locks just the bookings row. Every state change of a booking starts here. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> lockById(@Param("id") Long id);

    @Query("select b.id from Booking b where b.status = :status and b.expiresAt <= :now order by b.expiresAt")
    List<Long> findIdsToExpire(@Param("status") BookingStatus status, @Param("now") Instant now, Pageable pageable);

    @Query("""
            select b from Booking b
            where b.seeker.id = :seekerId and (:status is null or b.status = :status)
            """)
    Page<Booking> findBySeeker(@Param("seekerId") Long seekerId, @Param("status") BookingStatus status,
                               Pageable pageable);

    long countByStatus(BookingStatus status);

    @Query("""
            select coalesce(sum(b.totalAmount), 0) from Booking b
            where b.status in (com.krishiconnect.entity.BookingStatus.CONFIRMED,
                               com.krishiconnect.entity.BookingStatus.COMPLETED)
            """)
    BigDecimal sumBookedValue();

    @Query("select b from Booking b join fetch b.seeker where (:status is null or b.status = :status)")
    Page<Booking> findAllWithSeeker(@Param("status") BookingStatus status, Pageable pageable);
}
