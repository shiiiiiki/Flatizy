package org.flatizy.flatizy.entity.dto.user;

import lombok.Data;

@Data
public class UserDto {
    private String identificationNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
