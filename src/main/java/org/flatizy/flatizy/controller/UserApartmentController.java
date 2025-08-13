package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.service.UserApartmentService;
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
    public void save() {
        userApartmentService.save();
    }
}
