package org.flatizy.flatizy.entity.mapper;

import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.entity.dto.apartment.ExternalApartmentDto;
import org.flatizy.flatizy.entity.dto.apartment.ManualApartmentDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApartmentMapper {
    Apartment fromExternalDtoToEntity(ExternalApartmentDto.ApartmentDataDto apartmentDataDto);

    Apartment fromManualDtoToEntity(ManualApartmentDto.ApartmentDataDto apartmentDataDto);

    ManualApartmentDto.ApartmentDataDto fromEntityToManualDto(Apartment apartment);
}
