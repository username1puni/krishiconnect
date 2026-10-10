package com.krishiconnect.dto;

import com.krishiconnect.entity.Place;

public record PlaceResponse(Long id, String name, String district, double latitude, double longitude) {

    public static PlaceResponse from(Place p) {
        return new PlaceResponse(p.getId(), p.getName(), p.getDistrict(), p.getLatitude(), p.getLongitude());
    }
}
