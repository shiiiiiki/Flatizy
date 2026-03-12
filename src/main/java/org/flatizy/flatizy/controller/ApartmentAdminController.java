package org.flatizy.flatizy.controller;

import lombok.AllArgsConstructor;
import org.flatizy.flatizy.entity.dto.apartment.ApartmentRegistrationDto;
import org.flatizy.flatizy.service.apartment.ApartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/apartments/")
@AllArgsConstructor
public class ApartmentAdminController {

    private final ApartmentService apartmentService;

    @GetMapping("get/free")
    public ResponseEntity<List<ApartmentRegistrationDto.ApartmentDataDto>> getFreeApartments() {
        return ResponseEntity.ok(apartmentService.getFreeApartments());
    }
}
