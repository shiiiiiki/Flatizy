package org.flatizy.flatizy.entity.enums;

import lombok.Getter;

@Getter
public enum RequestType {
    ELEVATOR_NOT_WORKING("Ліфт не працює"),
    WATER_ISSUE("Проблема з водопостачанням"),
    HEATING_ISSUE("Проблема з опаленням"),
    ELECTRICITY_ISSUE("Проблема з електропостачанням"),
    PLUMBING("Сантехніка"),
    CLEANING("Прибирання"),
    REPAIR("Ремонт"),
    NOISE_COMPLAINT("Скарга на шум"),
    OTHER("Інше");

    private final String displayName;

    RequestType(String displayName) {
        this.displayName = displayName;
    }

}
