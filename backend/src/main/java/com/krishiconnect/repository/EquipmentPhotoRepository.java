package com.krishiconnect.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.krishiconnect.entity.EquipmentPhoto;

public interface EquipmentPhotoRepository extends JpaRepository<EquipmentPhoto, Long> {

    @Query("select p.id from EquipmentPhoto p where p.equipment.id = :equipmentId order by p.sortOrder, p.id")
    List<Long> findIdsByEquipmentId(@Param("equipmentId") Long equipmentId);

    /** First photo of each listing, for the result cards. Returns [equipmentId, photoId] pairs. */
    @Query("select p.equipment.id, min(p.id) from EquipmentPhoto p where p.equipment.id in :ids group by p.equipment.id")
    List<Object[]> findCoverPhotoIds(@Param("ids") Collection<Long> equipmentIds);

    long countByEquipmentId(Long equipmentId);
}
