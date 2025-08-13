package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Integer> {

    List<Apartment> findByTelegramName(String telegramName);

    Apartment findByApartmentNumberAndBuildingNumberAndHouseNumber(int apartNumber, int buildingNumber, int houseNumber);
}
