package org.flatizy.flatizy.entity.dto.apartment;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ExternalApartmentDto {

    private List<ApartmentDataDto> apartments;

    @Data
    @NoArgsConstructor
    public static class ApartmentDataDto {
        private Integer apartmentNumber;
        private Integer buildingNumber;
        private Integer houseNumber;
        private Double area;
        private String residentialComplex;
    }
}
