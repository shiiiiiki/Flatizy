package org.flatizy.flatizy.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    OWNER("owner"),
    ADMIN("admin"),
    TENANT("tenant");

    private final String value;

    UserRole(String string) {
        this.value = string;
    }

    @Override
    public String toString() {
        return value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UserRole from(String value) {
        return UserRole.valueOf(value.toUpperCase());
    }
}
