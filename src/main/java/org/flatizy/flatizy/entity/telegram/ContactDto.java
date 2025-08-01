package org.flatizy.flatizy.entity.telegram;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class ContactDto {
    private long id;
    private String username;
    private String first_name;
    private String last_name;
    private String phone;
    private boolean botStarted;

    public ContactDto(long id, String username, String firstName, String lastName, String phone, boolean botStarted) {
        this.id = id;
        this.username = username;
        this.first_name = firstName;
        this.last_name = lastName;
        this.phone = phone;
        this.botStarted = botStarted;
    }

}
