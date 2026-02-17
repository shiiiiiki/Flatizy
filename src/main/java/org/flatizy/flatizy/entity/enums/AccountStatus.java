package org.flatizy.flatizy.entity.enums;

public enum AccountStatus {
    SENT("Sent"),
    FAILED("Failed"),
    PENDING("Pending"),
    COMPLETED("Completed");

    private final String value;

    AccountStatus(String string) {
        this.value = string;
    }

    @Override
    public String toString() {
        return value;
    }
}
