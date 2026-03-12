package org.flatizy.flatizy.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptionService aesEncryptionService;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return aesEncryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return aesEncryptionService.decrypt(dbData);
    }
}