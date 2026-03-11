package org.flatizy.flatizy.entity.dto.apartment;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ApartmentRegistrationDto {

    private List<ApartmentRegistrationDto.ApartmentDataDto> apartments;

    @Data
    @NoArgsConstructor
    public static class ApartmentDataDto {
        private Integer id;
        private int apartmentNumber;
        private int buildingNumber;
        private int houseNumber;
        private String residentialComplex;
    }
}
