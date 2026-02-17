package org.flatizy.flatizy.service.apartment;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.dto.apartment.ExternalApartmentDto;
import org.flatizy.flatizy.entity.dto.response.ApartmentSaveResponse;
import org.flatizy.flatizy.entity.exception.ExternalApiContractException;
import org.flatizy.flatizy.entity.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ExternalApartmentService {

    @Value("${apartments.external.api.url}")
    private String externalApiUrl;
    private final RestTemplate restTemplate;
    private final ApartmentService apartmentService;

    public ExternalApartmentService(RestTemplate restTemplate, ApartmentService apartmentService) {
        this.restTemplate = restTemplate;
        this.apartmentService = apartmentService;
    }

    public ApartmentSaveResponse fetchAndSaveApartmentsFromExternalApi() {
        ExternalApartmentDto dto;
        try {
            dto = restTemplate.getForObject(externalApiUrl, ExternalApartmentDto.class);
        } catch (RestClientException e) {
            throw new ExternalApiException("Не удалось получить данные с внешнего API", e);
        }
        return saveApartmentsFromExternal(dto);
    }

    public ApartmentSaveResponse saveApartmentsFromExternal(ExternalApartmentDto requestDto) {
        if (requestDto == null || requestDto.getApartments() == null || requestDto.getApartments().isEmpty()) {
            throw new ExternalApiContractException("Receive empty apartments list");
        }
        return apartmentService.saveExternalApartments(requestDto.getApartments());
    }
}
