package com.krishiconnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.krishiconnect.entity.PriceUnit;
import com.krishiconnect.exception.BadRequestException;

/**
 * Price of one booked item.
 * HOUR: number of hours. DAY: number of calendar days the booking touches (6 AM to 6 PM is one
 * day, Monday 6 AM to Tuesday 6 PM is two). ACRE: the land size the farmer enters.
 */
@Component
public class PricingCalculator {

    public record Price(BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
    }

    public Price price(PriceUnit unit, BigDecimal rate, LocalDateTime start, LocalDateTime end, BigDecimal acres) {
        if (rate == null) {
            throw new BadRequestException("This listing has no price per " + unit.name().toLowerCase());
        }
        BigDecimal quantity = switch (unit) {
            case HOUR -> BigDecimal.valueOf(Duration.between(start, end).toHours());
            case DAY -> BigDecimal.valueOf(ChronoUnit.DAYS.between(start.toLocalDate(),
                    end.minusSeconds(1).toLocalDate()) + 1);
            case ACRE -> {
                if (acres == null || acres.signum() <= 0) {
                    throw new BadRequestException("Enter the land size in acres");
                }
                if (acres.compareTo(BigDecimal.valueOf(500)) > 0) {
                    throw new BadRequestException("Land size looks too large, please check");
                }
                yield acres.setScale(2, RoundingMode.HALF_UP);
            }
            case FIXED -> BigDecimal.ONE;
        };
        BigDecimal amount = quantity.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new Price(quantity, rate, amount);
    }
}
