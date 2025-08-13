package org.flatizy.flatizy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name = "apartments")
@Entity
public class Apartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
