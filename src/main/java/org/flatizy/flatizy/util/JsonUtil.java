package org.flatizy.flatizy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.flatizy.flatizy.entity.telegram.ContactDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonUtil {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void writeToJson(List<ContactDto> contactDtos, String filePath) {
        try {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(new File(filePath), contactDtos);
        } catch (Exception e) {
            logger.error("Failed to write contacts to JSON: {}", e.getMessage(), e);
        }
    }

    public static List<ContactDto> readFromJson(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            return mapper.readValue(file, mapper.getTypeFactory().constructCollectionType(List.class, ContactDto.class));
        } catch (Exception e) {
            logger.error("Failed to read contacts from JSON: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
