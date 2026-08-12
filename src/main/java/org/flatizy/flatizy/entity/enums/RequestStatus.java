package org.flatizy.flatizy.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RequestStatus {
    PENDING("В очікуванні"),
    IN_PROGRESS("В роботі"),
    COMPLETED("Виконано"),
    REJECTED("Відхилено");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
