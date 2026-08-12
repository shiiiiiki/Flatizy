package org.flatizy.flatizy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.flatizy.flatizy.entity.enums.UserRole;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"userApartments"})
@Table(name = "users")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    @Column(name = "identification_number", unique = true)
    private String identificationNumber;
    @Column(unique = true)
    private String password;
    @Column(unique = true)

    private String email;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;

    @Column(unique = true)
    private String phone;
    @Column(name = "bot_started")
    private Boolean botStarted;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;
    @LastModifiedDate
    @Column(name = "last_update")
    private LocalDateTime lastUpdate;
    @OneToMany(mappedBy = "user")
    private Set<UserApartment> userApartments;
}
