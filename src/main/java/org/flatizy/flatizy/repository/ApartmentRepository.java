package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Integer> {
    Optional<Apartment> findByApartmentNumberAndBuildingNumberAndHouseNumberAndResidentialComplex(Integer apartmentNumber, Integer buildingNumber, Integer houseNumber, String residentialComplex);
    List<Apartment> findByUserApartmentsIsEmpty();
}
