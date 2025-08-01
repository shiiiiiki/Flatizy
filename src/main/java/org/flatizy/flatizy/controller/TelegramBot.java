package org.flatizy.flatizy.controller;


import org.flatizy.flatizy.config.TelegramConfig;
import org.flatizy.flatizy.entity.telegram.ContactDto;
import org.flatizy.flatizy.service.TelegramBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Contact;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);
    private final TelegramConfig config;
    private final TelegramBotService telegramBotService;

    private boolean awaitingPhone = false;
    private boolean awaitingAccountNumber = false;

    private Long currentUserId;
    private String currentUsername;
    private String currentFirstName;
    private String currentLastName;
    private String currentPhone;

    public TelegramBot(TelegramConfig config, TelegramBotService telegramBotService) {
        this.config = config;
        this.telegramBotService = telegramBotService;
    }

    @Override
    public String getBotUsername() {
        return "FlatizyBot";
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                Long userId = message.getFrom().getId();
                String username = message.getFrom().getUserName();
                String chatId = message.getChatId().toString();

                if (message.hasText()) {
                    String text = message.getText();

                    if (text.equals("/start")) {
                        handleStart(userId, username, chatId);
                        return;
                    }

                    if (awaitingAccountNumber) {
                        String account = text.trim();
                        System.out.println("Лицевой счёт: " + account);

                        ContactDto newContact = new ContactDto(
                                currentUserId,
                                currentUsername != null ? "@" + currentUsername : "-",
                                currentFirstName != null ? currentFirstName : "-",
                                currentLastName != null ? currentLastName : "-",
                                currentPhone,
                                true
                        );

                        telegramBotService.addNewContact(newContact);
                        sendMessage(chatId, "✅ Регистрация завершена! Спасибо!");

                        awaitingAccountNumber = false;
                        resetCurrentData();
                        return;
                    }
                }

                if (message.hasContact() && awaitingPhone) {
                    Contact contact = message.getContact();
                    currentPhone = contact.getPhoneNumber();
                    currentFirstName = contact.getFirstName();
                    currentLastName = contact.getLastName();
                    currentUserId = contact.getUserId();
                    currentUsername = message.getFrom().getUserName();

                    awaitingPhone = false;
                    awaitingAccountNumber = true;

                    sendMessage(chatId, "📄 Пожалуйста, введите номер лицевого счёта:");
                }

            }

        } catch (Exception e) {
            logger.error("Error in update: {}", e.getMessage(), e);
        }
    }

    private void handleStart(Long userId, String username, String chatId) throws TelegramApiException {
        ContactDto contact = telegramBotService.findAndUpdateContact(userId);
        if (contact != null) {
            sendMessage(chatId, "👋 Добро пожаловать обратно! Ваши данные найдены.");
        } else {
            sendMessage(chatId, "👋 Вас нет в базе. Пожалуйста, поделитесь своим номером телефона:");

            KeyboardButton sharePhoneButton = new KeyboardButton("📱 Поделиться номером");
            sharePhoneButton.setRequestContact(true);
            KeyboardRow row = new KeyboardRow();
            row.add(sharePhoneButton);

            List<KeyboardRow> keyboard = new ArrayList<>();
            keyboard.add(row);

            ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
            markup.setKeyboard(keyboard);
            markup.setResizeKeyboard(true);
            markup.setOneTimeKeyboard(true);

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText("Нажмите кнопку ниже:");
            msg.setReplyMarkup(markup);
            execute(msg);

            awaitingPhone = true;
        }
    }

    private void sendMessage(String chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        execute(message);
    }

    private void resetCurrentData() {
        currentUserId = null;
        currentUsername = null;
        currentFirstName = null;
        currentLastName = null;
        currentPhone = null;
    }
}