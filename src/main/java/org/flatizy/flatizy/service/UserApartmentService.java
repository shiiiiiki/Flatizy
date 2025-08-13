package org.flatizy.flatizy.service;

import org.flatizy.flatizy.repository.UserApartmentRepository;
import org.springframework.stereotype.Service;

@Service
public class UserApartmentService {

    private final UserApartmentRepository userApartmentRepository;
    private final ApartmentService apartmentService;
    private final UserService userService;

    public UserApartmentService(UserApartmentRepository userApartmentRepository, ApartmentService apartmentService, UserService userService) {
        this.userApartmentRepository = userApartmentRepository;
        this.apartmentService = apartmentService;
        this.userService = userService;
    }

    public void save() {

    }
}
