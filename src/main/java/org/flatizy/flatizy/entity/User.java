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
@Table(name = "users")
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "telegram_id", unique = true, nullable = false)
    private Integer telegramId;
    private String userName;
    private String firstName;
    private String lastName;
    private String telegramName;
    private String phone;
    private Boolean botStarted;
    @OneToMany(mappedBy = "user")
    private Set<UserApartment> userApartments;

}
