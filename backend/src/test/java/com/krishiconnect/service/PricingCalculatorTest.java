package com.krishiconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.krishiconnect.entity.PriceUnit;
import com.krishiconnect.exception.BadRequestException;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();
    private final LocalDateTime six = LocalDateTime.of(2026, 11, 20, 6, 0);

    @Test
    void hourly() {
        var price = calculator.price(PriceUnit.HOUR, new BigDecimal("900"), six, six.plusHours(5), null);
        assertThat(price.quantity()).isEqualByComparingTo("5");
        assertThat(price.amount()).isEqualByComparingTo("4500");
    }

    @Test
    void dailyCountsCalendarDaysTouched() {
        // 6 AM to 6 PM is one day
        assertThat(calculator.price(PriceUnit.DAY, new BigDecimal("6500"), six, six.plusHours(12), null).quantity())
                .isEqualByComparingTo("1");
        // Friday 6 AM to Saturday 6 PM is two days
        assertThat(calculator.price(PriceUnit.DAY, new BigDecimal("6500"), six, six.plusHours(36), null).amount())
                .isEqualByComparingTo("13000");
        // ending exactly at midnight does not count the next day
        assertThat(calculator.price(PriceUnit.DAY, new BigDecimal("6500"), six, six.withHour(0).plusDays(1), null).quantity())
                .isEqualByComparingTo("1");
    }

    @Test
    void perAcreNeedsTheLandSize() {
        var price = calculator.price(PriceUnit.ACRE, new BigDecimal("1200"), six, six.plusHours(3), new BigDecimal("2.5"));
        assertThat(price.amount()).isEqualByComparingTo("3000");
        assertThatThrownBy(() -> calculator.price(PriceUnit.ACRE, new BigDecimal("1200"), six, six.plusHours(3), null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void fixedQuoteAmountIsUsedAsIs() {
        var price = calculator.price(PriceUnit.FIXED, new BigDecimal("9000"), six, six.plusHours(5), null);
        assertThat(price.amount()).isEqualByComparingTo("9000");
    }

    @Test
    void missingRateIsAClearError() {
        assertThatThrownBy(() -> calculator.price(PriceUnit.DAY, null, six, six.plusHours(5), null))
                .hasMessageContaining("per day");
    }
}
