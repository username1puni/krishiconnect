package com.krishiconnect.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.BookingItem;
import com.krishiconnect.entity.BookingItemStatus;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    /** Two ranges overlap when each one starts before the other ends. */
    @Query("""
            select count(i) > 0 from BookingItem i
            where i.equipment.id = :equipmentId and i.status in :statuses
              and i.startAt < :endAt and i.endAt > :startAt
            """)
    boolean existsEquipmentOverlap(@Param("equipmentId") Long equipmentId, @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt,
                                   @Param("statuses") Collection<BookingItemStatus> statuses);

    @Query("""
            select count(i) > 0 from BookingItem i
            where i.specialist.id = :specialistId and i.status in :statuses
              and i.startAt < :endAt and i.endAt > :startAt
            """)
    boolean existsSpecialistOverlap(@Param("specialistId") Long specialistId, @Param("startAt") LocalDateTime startAt,
                                    @Param("endAt") LocalDateTime endAt,
                                    @Param("statuses") Collection<BookingItemStatus> statuses);

    @Query("""
            select i from BookingItem i
            where i.equipment.id = :equipmentId and i.status in :statuses
              and i.startAt < :to and i.endAt > :from
            order by i.startAt
            """)
    List<BookingItem> findEquipmentBusy(@Param("equipmentId") Long equipmentId, @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        @Param("statuses") Collection<BookingItemStatus> statuses);

    @Query("""
            select i from BookingItem i
            where i.specialist.id = :specialistId and i.status in :statuses
              and i.startAt < :to and i.endAt > :from
            order by i.startAt
            """)
    List<BookingItem> findSpecialistBusy(@Param("specialistId") Long specialistId, @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         @Param("statuses") Collection<BookingItemStatus> statuses);

    @Query(value = """
            select i from BookingItem i join fetch i.booking b
            where i.provider.id = :providerId and (:status is null or i.status = :status)
            """, countQuery = """
            select count(i) from BookingItem i
            where i.provider.id = :providerId and (:status is null or i.status = :status)
            """)
    Page<BookingItem> findForProvider(@Param("providerId") Long providerId, @Param("status") BookingItemStatus status,
                                      Pageable pageable);

    long countByProviderIdAndStatus(Long providerId, BookingItemStatus status);
}
