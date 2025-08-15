package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.service.UserApartmentService;
import org.flatizy.flatizy.service.UserApartmentService.ManualMappingLists;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usap")
public class UserApartmentController {

    public final UserApartmentService userApartmentService;

    public UserApartmentController(UserApartmentService userApartmentService) {
        this.userApartmentService = userApartmentService;
    }

    @PostMapping
    public ResponseEntity<ManualMappingLists> save() {
        return new ResponseEntity<>(userApartmentService.save(), HttpStatus.OK);
    }
}
