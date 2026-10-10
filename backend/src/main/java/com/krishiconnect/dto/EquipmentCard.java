package com.krishiconnect.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.location.Distance;
import com.krishiconnect.location.DistanceSource;

/** One result in the equipment list. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EquipmentCard(Long id, String title, EquipmentCategory category, String brand, String model,
                            Integer horsePower, BigDecimal pricePerHour, BigDecimal pricePerDay,
                            BigDecimal pricePerAcre, boolean withOperator, String village, String district,
                            double latitude, double longitude, Long ownerId, String ownerName,
                            boolean ownerVerified, Double rating, int ratingCount, Long coverPhotoId,
                            Double distanceKm, Integer durationMinutes, DistanceSource distanceSource) {

    public static EquipmentCard from(Equipment e, Long coverPhotoId, Distance distance) {
        return new EquipmentCard(e.getId(), e.getTitle(), e.getCategory(), e.getBrand(), e.getModel(),
                e.getHorsePower(), e.getPricePerHour(), e.getPricePerDay(), e.getPricePerAcre(), e.isWithOperator(),
                e.getVillage(), e.getDistrict(), e.getLatitude(), e.getLongitude(), e.getOwner().getId(),
                e.getOwner().getFullName(), e.getOwner().isVerified(), e.averageRating(), e.getRatingCount(),
                coverPhotoId, distance == null ? null : distance.km(), distance == null ? null : distance.minutes(),
                distance == null ? null : distance.source());
    }
}
