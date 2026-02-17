package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.handler.TelegramUpdateHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequestMapping("/telegram")
public class TelegramWebhookController {

    private final TelegramUpdateHandler updateHandler;

    public TelegramWebhookController(TelegramUpdateHandler updateHandler) {
        this.updateHandler = updateHandler;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> onUpdateReceived(@RequestBody Update update) {
        updateHandler.handle(update);
        return ResponseEntity.ok().build();
    }
}
