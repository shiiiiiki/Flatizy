package org.flatizy.flatizy.entity.dto;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import lombok.Data;
import org.flatizy.flatizy.entity.Account;
import org.flatizy.flatizy.entity.UserApartment;

import java.util.Set;

@Data
public class ApartmentDto {

    private Integer id;
    private int apartmentNumber;
    private int buildingNumber;
    private int houseNumber;
    private String telegramName;
    private String residentialComplex;


    @OneToMany(mappedBy = "apartment")
    private Set<UserApartment> userApartments;

    @OneToMany(mappedBy = "apartment", cascade = CascadeType.ALL)
    private Set<Account> accounts;
}
