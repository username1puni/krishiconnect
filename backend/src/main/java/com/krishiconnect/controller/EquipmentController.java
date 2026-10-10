package com.krishiconnect.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.krishiconnect.dto.EquipmentCard;
import com.krishiconnect.dto.EquipmentDetail;
import com.krishiconnect.dto.EquipmentRequest;
import com.krishiconnect.dto.EquipmentStatusRequest;
import com.krishiconnect.dto.NearbyPageResponse;
import com.krishiconnect.entity.EquipmentCategory;
import com.krishiconnect.security.AuthUser;
import com.krishiconnect.service.EquipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "Equipment")
@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @Operation(summary = "Browse equipment. With lat/lng the results are sorted by road distance "
            + "(straight line when the routing API is not available).")
    @GetMapping
    public NearbyPageResponse<EquipmentCard> search(@RequestParam(required = false) Double lat,
                                                    @RequestParam(required = false) Double lng,
                                                    @RequestParam(required = false) Double radiusKm,
                                                    @RequestParam(required = false) EquipmentCategory category,
                                                    @RequestParam(defaultValue = "distance") String sort,
                                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return equipmentService.search(new EquipmentService.SearchQuery(lat, lng, radiusKm, category, sort), page, size);
    }

    @GetMapping("/{id}")
    public EquipmentDetail get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        return equipmentService.get(id, me);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PROVIDER')")
    public List<EquipmentDetail> mine(@AuthenticationPrincipal AuthUser me) {
        return equipmentService.mine(me.getId());
    }

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<EquipmentDetail> create(@AuthenticationPrincipal AuthUser me,
                                                  @Valid @RequestBody EquipmentRequest request) {
        EquipmentDetail created = equipmentService.create(me.getId(), request);
        return ResponseEntity.created(URI.create("/api/equipment/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public EquipmentDetail update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                  @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.update(me.getId(), id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PROVIDER')")
    public EquipmentDetail status(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                  @Valid @RequestBody EquipmentStatusRequest request) {
        return equipmentService.setStatus(me.getId(), id, request.status());
    }

    @PostMapping(value = "/{id}/photos", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('PROVIDER')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> addPhoto(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                      @RequestParam("file") MultipartFile file) {
        return Map.of("id", equipmentService.addPhoto(me.getId(), id, file));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @PreAuthorize("hasRole('PROVIDER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @PathVariable Long photoId) {
        equipmentService.deletePhoto(me.getId(), id, photoId);
    }
}
