package com.krishiconnect.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishiconnect.entity.Equipment;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.entity.EquipmentStatus;
import com.krishiconnect.entity.ModerationStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EquipmentDetail(Long id, String title, EquipmentCategory category, String brand, String model,
                              Integer yearOfMake, Integer horsePower, String description, BigDecimal pricePerHour,
                              BigDecimal pricePerDay, BigDecimal pricePerAcre, boolean withOperator,
                              String village, String district, double latitude, double longitude,
                              Long ownerId, String ownerName, String ownerVillage, boolean ownerVerified,
                              Double rating, int ratingCount, List<Long> photoIds, EquipmentStatus status,
                              ModerationStatus moderationStatus, String moderationNote, Instant createdAt) {

    public static EquipmentDetail from(Equipment e, List<Long> photoIds) {
        return new EquipmentDetail(e.getId(), e.getTitle(), e.getCategory(), e.getBrand(), e.getModel(),
                e.getYearOfMake(), e.getHorsePower(), e.getDescription(), e.getPricePerHour(), e.getPricePerDay(),
                e.getPricePerAcre(), e.isWithOperator(), e.getVillage(), e.getDistrict(), e.getLatitude(),
                e.getLongitude(), e.getOwner().getId(), e.getOwner().getFullName(), e.getOwner().getVillage(),
                e.getOwner().isVerified(), e.averageRating(), e.getRatingCount(), photoIds, e.getStatus(),
                e.getModerationStatus(), e.getModerationNote(), e.getCreatedAt());
    }
}
