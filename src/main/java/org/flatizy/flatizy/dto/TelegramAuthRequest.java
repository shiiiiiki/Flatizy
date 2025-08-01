package org.flatizy.flatizy.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TelegramAuthRequest {
    private String phoneNumber;
    private String authCode;

}
