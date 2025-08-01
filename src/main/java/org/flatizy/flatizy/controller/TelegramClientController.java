package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.service.TelegramClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telegram")
public class TelegramClientController {
    private static final Logger logger = LoggerFactory.getLogger(TelegramClientController.class);
    private final TelegramClientService telegramService;

    @Autowired
    public TelegramClientController(TelegramClientService telegramService) {
        this.telegramService = telegramService;
    }

    @PostMapping("/contacts")
    public ResponseEntity<String> extractContacts() {
        try {
            telegramService.processContactsAndFiles();
            return ResponseEntity.ok("Contacts extracted and saved to contacts.json");
        } catch (Exception e) {
            logger.error("Error extracting contacts: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to extract contacts: " + e.getMessage());
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<String> auth() {
        try {
            telegramService.auth();
            return ResponseEntity.ok("Auth success");
        } catch (Exception e) {
            logger.error("Error during auth: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error during auth: " + e.getMessage());
        }
    }
}
