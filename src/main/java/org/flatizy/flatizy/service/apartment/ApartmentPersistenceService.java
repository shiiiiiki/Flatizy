package org.flatizy.flatizy.service.apartment;

import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.repository.ApartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApartmentPersistenceService {

    private final ApartmentRepository apartmentRepository;

    public ApartmentPersistenceService(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    @Transactional
    public Apartment saveOrUpdate(Apartment apartment) {
        Apartment apartmentToSave =
                apartmentRepository.findByApartmentNumberAndBuildingNumberAndHouseNumberAndResidentialComplex(
                        apartment.getApartmentNumber(), apartment.getBuildingNumber(),
                        apartment.getHouseNumber(), apartment.getResidentialComplex()
                ).orElseGet(Apartment::new);

        apartmentToSave.setApartmentNumber(apartment.getApartmentNumber());
        apartmentToSave.setBuildingNumber(apartment.getBuildingNumber());
        apartmentToSave.setHouseNumber(apartment.getHouseNumber());
        apartmentToSave.setResidentialComplex(apartment.getResidentialComplex());

        if (apartment.getArea() != null && apartment.getArea() > 0) {
            apartmentToSave.setArea(apartment.getArea());
        }

        return apartmentRepository.save(apartmentToSave);
    }
}
