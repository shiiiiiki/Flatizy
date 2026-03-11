package org.flatizy.flatizy.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum RequestStatus {
    PENDING("В ожидании"),
    IN_PROGRESS("В работе"),
    COMPLETED("Выполнено"),
    REJECTED("Отклонено");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getName() {
        return name();
    }
}
