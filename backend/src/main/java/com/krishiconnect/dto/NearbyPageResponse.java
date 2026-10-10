package com.krishiconnect.dto;

import java.util.List;

/** A page of distance sorted results plus how many distances came from the road API. */
public record NearbyPageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages,
                                    int roadDistances, int straightLineDistances) {

    public static <T> NearbyPageResponse<T> of(PageResponse<T> page, int road, int straight) {
        return new NearbyPageResponse<>(page.content(), page.page(), page.size(), page.totalElements(),
                page.totalPages(), road, straight);
    }
}
