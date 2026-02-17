package org.flatizy.flatizy.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.flatizy.flatizy.handler.TelegramUpdateHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramWebhookBot {

    private final TelegramUpdateHandler updateHandler;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.webhook-path}")
    private String webhookPath;

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotPath() {
        return webhookPath;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        updateHandler.handle(update);
        return null;
    }

    @PostConstruct
    public void init() {
        updateHandler.setBot(this);
    }
}
