package org.flatizy.flatizy.entity.enums;

import lombok.Getter;

@Getter
public enum RequestType {
    ELEVATOR_NOT_WORKING("Лифт не работает"),
    WATER_ISSUE("Проблема с водой"),
    HEATING_ISSUE("Проблема с отоплением"),
    ELECTRICITY_ISSUE("Проблема с электричеством"),
    PLUMBING("Сантехника"),
    CLEANING("Уборка"),
    REPAIR("Ремонт"),
    NOISE_COMPLAINT("Жалоба на шум"),
    OTHER("Другое");

    private final String displayName;

    RequestType(String displayName) {
        this.displayName = displayName;
    }

}
