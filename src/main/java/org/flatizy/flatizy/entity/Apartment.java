package org.flatizy.flatizy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"userApartments", "accounts"})
@Table(name = "apartments")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Apartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private int apartmentNumber;
    private int buildingNumber;
    private int houseNumber;
    private Double area;
    private String residentialComplex;
    @Column(name = "last_update")
    @LastModifiedDate
    private LocalDateTime lastUpdate;
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "apartment")
    private Set<UserApartment> userApartments;

    @OneToMany(mappedBy = "apartment", cascade = CascadeType.ALL)
    private Set<Account> accounts;

}
