package org.flatizy.flatizy.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.handler.TelegramUpdateHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBot extends TelegramWebhookBot {

    private final TelegramUpdateHandler updateHandler;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.webhook-path}")
    private String webhookPath;

    @Value("${telegram.webhook.url:}")
    private String webhookUrl;

    @Value("${telegram.webhook.secret-token:}")
    private String secretToken;

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

        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            registerWebhook();
        }
    }

    private void registerWebhook() {
        try {
            SetWebhook setWebhook = SetWebhook.builder()
                    .url(webhookUrl)
                    .secretToken(secretToken)
                    .build();

            execute(setWebhook);
            log.info("Telegram webhook registered successfully at: {}", webhookUrl);
        } catch (TelegramApiException e) {
            log.error("Failed to register webhook", e);
        }
    }
}
