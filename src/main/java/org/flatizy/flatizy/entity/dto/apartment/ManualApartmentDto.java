package org.flatizy.flatizy.entity.dto.apartment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ManualApartmentDto {

    private List<ManualApartmentDto.ApartmentDataDto> apartments;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApartmentDataDto {
        private int apartmentNumber;
        private int buildingNumber;
        private int houseNumber;
        private double area;
        private String residentialComplex;
    }
}

