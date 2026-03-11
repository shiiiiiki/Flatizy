package org.flatizy.flatizy.service.apartment;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.entity.dto.apartment.ApartmentRegistrationDto;
import org.flatizy.flatizy.entity.dto.apartment.ExternalApartmentDto;
import org.flatizy.flatizy.entity.dto.apartment.ManualApartmentDto;
import org.flatizy.flatizy.entity.dto.response.ApartmentSaveResponse;
import org.flatizy.flatizy.entity.mapper.ApartmentMapper;
import org.flatizy.flatizy.repository.ApartmentRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ApartmentService {
    private final ApartmentRepository apartmentRepository;
    private final ApartmentMapper apartmentMapper;
    private final ApartmentPersistenceService apartmentPersistenceService;

    public ApartmentService(ApartmentRepository apartmentRepository, ApartmentMapper apartmentMapper, ApartmentPersistenceService apartmentPersistenceService) {
        this.apartmentRepository = apartmentRepository;
        this.apartmentMapper = apartmentMapper;
        this.apartmentPersistenceService = apartmentPersistenceService;
    }

    public List<Apartment> getAll() {
        try {
            return apartmentRepository.findAll();
        } catch (DataAccessException e) {
            throw new RuntimeException("Exception during fetching apartments: " + e.getMessage(), e);
        }
    }

    public List<ApartmentRegistrationDto.ApartmentDataDto> getFreeApartments() {
        return apartmentRepository.findByUserApartmentsIsEmpty()
                .stream()
                .map(apartmentMapper::fromEntityToRegistrationDto)
                .toList();
    }

    public List<ManualApartmentDto.ApartmentDataDto> getAllAsDto() {
        try {
            return apartmentRepository.findAll().stream()
                    .map(apartmentMapper::fromEntityToManualDto)
                    .toList();
        } catch (DataAccessException e) {
            throw new RuntimeException("Exception during fetching apartments: " + e.getMessage(), e);
        }
    }

    public ApartmentSaveResponse saveApartmentsManually(ManualApartmentDto dto) {
        List<Apartment> list = dto.getApartments().stream()
                .map(apartmentMapper::fromManualDtoToEntity)
                .toList();
        return saveApartments(list);
    }

    public ApartmentSaveResponse saveExternalApartments(List<ExternalApartmentDto.ApartmentDataDto> dto) {
        List<Apartment> list = dto.stream()
                .map(apartmentMapper::fromExternalDtoToEntity)
                .toList();
        return saveApartments(list);
    }

    public ApartmentSaveResponse saveApartments(List<Apartment> apartmentDataList) {
        int savedCount = 0;
        int skippedCount = 0;

        for (Apartment apartment : apartmentDataList) {
            if (!isValidApartment(apartment)) {
                log.warn("Пропуск квартиры с невалидными данными: {}", apartment);
                skippedCount++;
                continue;
            }
            try {
                apartment = apartmentPersistenceService.saveOrUpdate(apartment);
                savedCount++;
                log.info("Квартира сохранена: {}", apartment);
            } catch (DataAccessException e) {
                log.error("Ошибка {} при сохранении квартиры: {}", e.getMessage(), apartment);
                skippedCount++;
            }
        }
        boolean success = savedCount > 0;
        String message = success ? "Обработка завершена" : "Не удалось сохранить ни одной квартиры";

        return ApartmentSaveResponse.builder()
                .success(success)
                .message(message)
                .saved(savedCount)
                .skipped(skippedCount)
                .build();
    }

    private boolean isValidApartment(Apartment apartment) {
        return apartment.getApartmentNumber() > 0 &&
                apartment.getBuildingNumber() > 0 &&
                apartment.getHouseNumber() >= 0;
    }

    public List<Apartment> findAllById(List<Integer> apartmentIds) {
        return apartmentRepository.findAllById(apartmentIds);
    }

    public Optional<Apartment> findById(Integer id) {
        return apartmentRepository.findById(id);
    }
}
