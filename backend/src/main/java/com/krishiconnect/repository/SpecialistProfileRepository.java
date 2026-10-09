package com.krishiconnect.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.SpecialistProfile;

import jakarta.persistence.LockModeType;

public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long> {

    /**
     * SELECT ... FOR UPDATE on the profile rows only (no join), in id order, so two bookings
     * that want the same people always lock them in the same order.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SpecialistProfile p where p.userId in :ids order by p.userId")
    List<SpecialistProfile> lockAllByIds(@Param("ids") Collection<Long> ids);

    @Query("select p from SpecialistProfile p join fetch p.user where p.userId in :ids")
    List<SpecialistProfile> findAllWithUser(@Param("ids") Collection<Long> ids);
}
