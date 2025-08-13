package org.flatizy.flatizy.service;

import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.repository.ApartmentRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Objects;

@Service
public class ApartmentService {
    private final ApartmentRepository apartmentRepository;
    private final FileService fileService;
    private final AccountService accountService;

    public ApartmentService(ApartmentRepository apartmentRepository, FileService fileService, AccountService accountService) {
        this.apartmentRepository = apartmentRepository;
        this.fileService = fileService;
        this.accountService = accountService;
    }

    public List<Apartment> getAll() {
        try {
            return apartmentRepository.findAll();
        } catch (DataAccessException e) {
            throw new RuntimeException("Exception during fetching apartments: " + e.getMessage(), e);
        }
    }

    public void save(File file) {
        String fileText = fileService.parsePdfFile(file);
        //todo remove with more accurate check
        int buildingNumber = fileService.extractNumberAfter(fileText, "корпус");
        int apartNumber = fileService.extractNumberAfter(fileText, "кв.");
        int accNumber = fileService.extractNumberAfter(fileText, "рахунку");
        int houseNumber = fileService.extractNumberAfter(fileText, "буд");
        String telegramName = fileService.createTelegramName(buildingNumber, apartNumber);

        if (buildingNumber <= 0 || apartNumber < 0 || houseNumber < 0) {
            System.out.println("Невозможно извлечь корпус или квартиру из файла: " + file.getName());
            return;
        }

        Apartment apartment;
        Apartment savedApartment = apartmentRepository.findByApartmentNumberAndBuildingNumberAndHouseNumber(apartNumber, buildingNumber, houseNumber);
        apartment = Objects.requireNonNullElseGet(savedApartment, Apartment::new);

        apartment.setBuildingNumber(buildingNumber);
        apartment.setApartmentNumber(apartNumber);
        apartment.setHouseNumber(houseNumber);
        apartment.setResidentialComplex("Прохоровський квартал");
        apartment.setTelegramName(telegramName);

        try {
            apartment = apartmentRepository.save(apartment);
            System.out.println("Apartment saved" + apartment);
        } catch (DataAccessException e) {
            throw new RuntimeException("Exception during saving apartment: " + e);
        }
        if (accNumber > 0) {
            accountService.saveAccount(accNumber, apartment);
        }
    }


}
