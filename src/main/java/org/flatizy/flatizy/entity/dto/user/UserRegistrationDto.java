package org.flatizy.flatizy.entity.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.flatizy.flatizy.entity.enums.UserRole;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class UserRegistrationDto {

    private String identificationNumber;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private UserRole role;

    private List<Integer> apartmentIds;
}
