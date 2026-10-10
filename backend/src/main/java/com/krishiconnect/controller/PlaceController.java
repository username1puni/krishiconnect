package com.krishiconnect.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.PlaceResponse;
import com.krishiconnect.repository.PlaceRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Places")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceRepository placeRepository;

    @GetMapping
    public List<PlaceResponse> search(@RequestParam(required = false) String q) {
        var places = q == null || q.isBlank()
                ? placeRepository.findAllByOrderByDistrictAscNameAsc()
                : placeRepository.findTop20ByNameStartingWithIgnoreCaseOrderByName(q.trim());
        return places.stream().map(PlaceResponse::from).toList();
    }

    /** Used to show "near Mandya" after the browser gives us raw GPS coordinates. */
    @GetMapping("/nearest")
    public ResponseEntity<PlaceResponse> nearest(@RequestParam double lat, @RequestParam double lng) {
        return placeRepository.findNearest(lat, lng).map(PlaceResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
