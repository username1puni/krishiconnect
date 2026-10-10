package com.krishiconnect.controller;

import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.entity.EquipmentPhoto;
import com.krishiconnect.service.EquipmentService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Equipment")
@RestController
@RequiredArgsConstructor
public class PhotoController {

    private final EquipmentService equipmentService;

    /** Photos never change once uploaded (a new upload gets a new id), so browsers can cache them. */
    @GetMapping("/api/photos/{id}")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        EquipmentPhoto photo = equipmentService.photo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(photo.getData());
    }
}
