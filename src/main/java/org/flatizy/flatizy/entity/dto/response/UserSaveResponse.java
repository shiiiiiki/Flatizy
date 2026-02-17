package org.flatizy.flatizy.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSaveResponse {
    private boolean success;
    private String message;
}
