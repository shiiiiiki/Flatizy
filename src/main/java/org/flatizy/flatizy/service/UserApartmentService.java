package org.flatizy.flatizy.service;

import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.UserApartment;
import org.flatizy.flatizy.repository.UserApartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserApartmentService {

    private final UserApartmentRepository userApartmentRepository;

    public UserApartmentService(UserApartmentRepository userApartmentRepository) {
        this.userApartmentRepository = userApartmentRepository;
    }

    public void saveAll(List<UserApartment> userApartments){
        userApartmentRepository.saveAll(userApartments);
    }

    public void linkUserToApartment(User user, Apartment apartment) {
        UserApartment userApartment = new UserApartment();
        userApartment.setUser(user);
        userApartment.setApartment(apartment);
        userApartmentRepository.save(userApartment);
    }

    public List<Apartment> getApartmentsByUser(User user) {
        return userApartmentRepository.findAllByUser(user)
                .stream()
                .map(UserApartment::getApartment)
                .collect(Collectors.toList());
    }
}
