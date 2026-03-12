package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.entity.dto.apartment.ExternalApartmentDto;
import org.flatizy.flatizy.entity.dto.apartment.ManualApartmentDto;
import org.flatizy.flatizy.entity.dto.response.ApartmentSaveResponse;
import org.flatizy.flatizy.service.apartment.ApartmentService;
import org.flatizy.flatizy.service.apartment.ExternalApartmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apartments/")
public class ApartmentController {

    private final ApartmentService apartmentService;
    private final ExternalApartmentService externalApartmentService;

    public ApartmentController(ApartmentService apartmentService, ExternalApartmentService externalApartmentService) {
        this.apartmentService = apartmentService;
        this.externalApartmentService = externalApartmentService;
    }

    @GetMapping("get")
    public ResponseEntity<List<ManualApartmentDto.ApartmentDataDto>> getAll() {
        return ResponseEntity.ok(apartmentService.getAllAsDto());
    }


    @PostMapping("save-manual")
    public ResponseEntity<ApartmentSaveResponse> saveApartmentManual(@RequestBody ManualApartmentDto apartments) {
        ApartmentSaveResponse response = apartmentService.saveApartmentsManually(apartments);
        return ResponseEntity.ok(response);
    }

    @PostMapping("fetch-save-external")
    public ResponseEntity<ApartmentSaveResponse> fetchAndSaveApartmentsFromExternal() {
        ApartmentSaveResponse response = externalApartmentService.fetchAndSaveApartmentsFromExternalApi();
        return ResponseEntity.ok(response);
    }

    @PostMapping("push")
    public ResponseEntity<ApartmentSaveResponse> pushApartmentsFromBuilder(
            @RequestBody ExternalApartmentDto requestDto) {
        ApartmentSaveResponse response = externalApartmentService.saveApartmentsFromExternal(requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fetch-external-mock")
    public String fetchMockJson() {
        return """
    {
      "apartments": [
        {
          "apartmentNumber": 103,
          "buildingNumber": 0,
          "houseNumber": 5,
          "area": 55.0,
          "residentialComplex": "ЖК Премиум",
          "ceilingHeight": 2.7,
          "rooms": 2,
          "floor": 3,
          "hasBalcony": true,
          "furnishingType": "premium",
          "views": "city",
          "parking": true,
          "renovationDate": "2023-01-15"
        }
      ]
    }
    """;
    }

}
