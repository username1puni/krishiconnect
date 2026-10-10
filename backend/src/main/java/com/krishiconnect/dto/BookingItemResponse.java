package com.krishiconnect.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishiconnect.entity.BookingItem;
import com.krishiconnect.entity.BookingItemStatus;
import com.krishiconnect.entity.PriceUnit;
import com.krishiconnect.entity.ResourceType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingItemResponse(Long id, ResourceType resourceType, Long resourceId, String resourceName,
                                  String category, Long providerId, String providerName, String providerPhone,
                                  LocalDateTime startAt, LocalDateTime endAt, PriceUnit priceUnit,
                                  BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount,
                                  BookingItemStatus status) {

    /** The provider's phone number is shared once the booking is confirmed. */
    public static BookingItemResponse from(BookingItem i, boolean showPhone) {
        String category = i.getEquipment() != null ? i.getEquipment().getCategory().name() : null;
        return new BookingItemResponse(i.getId(), i.getResourceType(), i.resourceId(), i.resourceName(), category,
                i.getProvider().getId(), i.getProvider().getFullName(), showPhone ? i.getProvider().getPhone() : null,
                i.getStartAt(), i.getEndAt(), i.getPriceUnit(), i.getQuantity(), i.getUnitPrice(), i.getAmount(),
                i.getStatus());
    }
}
