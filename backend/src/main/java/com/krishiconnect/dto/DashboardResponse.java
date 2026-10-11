package com.krishiconnect.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.krishiconnect.location.RoutingStatus;

public record DashboardResponse(Map<String, Long> usersByRole, long pendingVerifications, long equipmentListed,
                                long pendingListings, long openRequirements, Map<String, Long> bookingsByStatus,
                                BigDecimal bookedValue, List<MonthStat> monthly,
                                List<CategoryMonth> seasonalDemand, List<CategoryCount> topCategories,
                                RoutingStatus routing) {

    /** Bookings and their value per month (confirmed and completed only). */
    public record MonthStat(String month, long bookings, BigDecimal value) {
    }

    /** Booked equipment per category per month, for the seasonal demand chart. */
    public record CategoryMonth(String month, String category, long bookings) {
    }

    public record CategoryCount(String category, long bookings) {
    }
}
