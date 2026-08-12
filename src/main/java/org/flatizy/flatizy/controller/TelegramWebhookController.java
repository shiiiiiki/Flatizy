package org.flatizy.flatizy.controller;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.handler.TelegramUpdateHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
@RequestMapping("/telegram")
@Slf4j
public class TelegramWebhookController {

    private final TelegramUpdateHandler updateHandler;

    public TelegramWebhookController(TelegramUpdateHandler updateHandler) {
        this.updateHandler = updateHandler;
    }
    @PostMapping("/webhook")
    public ResponseEntity<Void> onUpdateReceived(@RequestBody Update update) {
        if (update == null || update.getUpdateId() == null) {
            log.warn("Invalid update structure received: update is null or has no updateId");
            return ResponseEntity.badRequest().build();
        }

        logUpdateSafely(update);

        try {
            updateHandler.handle(update);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook update: {}", update.getUpdateId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void logUpdateSafely(Update update) {
        try {
            String updateType = getUpdateType(update);
            Long userId = getUserIdFromUpdate(update);

            log.debug("Webhook update received: updateId={}, userId={}, type={}",
                     update.getUpdateId(),
                     userId,
                     updateType);
        } catch (Exception e) {
            log.debug("Error logging update: {}", e.getMessage());
        }
    }

    private String getUpdateType(Update update) {
        if (update.hasMessage()) return "MESSAGE";
        if (update.hasCallbackQuery()) return "CALLBACK";
        if (update.hasInlineQuery()) return "INLINE";
        if (update.hasEditedMessage()) return "EDITED_MESSAGE";
        if (update.hasChannelPost()) return "CHANNEL_POST";
        return "UNKNOWN";
    }

    private Long getUserIdFromUpdate(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getId();
        }
        return null;
    }
}
