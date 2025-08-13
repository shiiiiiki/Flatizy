package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.service.ApartmentService;
import org.flatizy.flatizy.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/apartments/")
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final FileService fileService;

    private static final String PATH_FILE = "src/main/resources/files/";

    public ApartmentController(ApartmentService apartmentService, FileService fileService) {
        this.apartmentService = apartmentService;
        this.fileService = fileService;
    }

    @GetMapping("get")
    public ResponseEntity<List<Apartment>> getAll() {
        return ResponseEntity.ok(apartmentService.getAll());
    }

    @PostMapping("save")
    public ResponseEntity<Void> saveApartments() {
        fileService.processFiles(PATH_FILE, apartmentService::save);
        return ResponseEntity.ok().build();
    }

}
