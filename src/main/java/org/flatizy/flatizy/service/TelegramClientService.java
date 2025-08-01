package org.flatizy.flatizy.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flatizy.flatizy.config.TelegramConfig;
import org.flatizy.flatizy.entity.telegram.ContactDto;
import org.flatizy.flatizy.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

@Service
public class TelegramClientService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramClientService.class);
    private final TelegramConfig config;
    private final ObjectMapper objectMapper;


    public TelegramClientService(TelegramConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public void processContactsAndFiles() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                config.getPythonPath(),
                config.getPythonContactsScriptPath(),
                config.getApiId(),
                config.getApiHash()
        );
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.redirectErrorStream(false);
        Process process = pb.start();


        BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();

        String line;
        while ((line = stdOut.readLine()) != null) {
            output.append(line).append("\n");
        }
        while ((line = stdErr.readLine()) != null) {
            errors.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Python script failed with exit code {}: {}", exitCode, errors);
            throw new RuntimeException("Python script failed: " + errors);
        }

        List<ContactDto> contactDtos = objectMapper.readValue(output.toString(), new TypeReference<>() {
        });
//        contacts.stream().filter(contact -> contact.getFirstName()!=null).forEach(System.out::println);
        logger.info("Retrieved {} contacts from Python script", contactDtos.size());

        JsonUtil.writeToJson(contactDtos, config.getContactsPath());
    }

    public void auth() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    config.getPythonPath(),
                    config.getPythonAuthScriptPath(),
                    config.getApiId(),
                    config.getApiHash()
            );
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            pb.inheritIO();
            Process process = pb.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Ошибка авторизации. Код выхода: " + exitCode);
            }

        } catch (Exception e) {
            logger.error("Авторизация через Telegram не удалась", e);
            throw new RuntimeException("Авторизация через Telegram не удалась: " + e.getMessage(), e);
        }
    }
}