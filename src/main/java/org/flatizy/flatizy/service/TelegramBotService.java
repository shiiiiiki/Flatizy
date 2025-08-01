package org.flatizy.flatizy.service;

import org.flatizy.flatizy.config.TelegramConfig;
import org.flatizy.flatizy.entity.telegram.ContactDto;
import org.flatizy.flatizy.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TelegramBotService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotService.class);
    TelegramConfig telegramConfig;

    public TelegramBotService(TelegramConfig telegramConfig) {
        this.telegramConfig = telegramConfig;
    }

    public ContactDto findAndUpdateContact(long userId) {
        List<ContactDto> contactDtos = JsonUtil.readFromJson(telegramConfig.getContactsPath());
        for (ContactDto contactDto : contactDtos) {
            if (contactDto.getId() == userId) {
                contactDto.setBotStarted(true);
                JsonUtil.writeToJson(contactDtos, telegramConfig.getContactsPath());
                logger.info("Updated contact: ID={}, Username={}, FirstName={}, LastName={}, Phone={}, BotStarted={}",
                        contactDto.getId(), contactDto.getUsername(), contactDto.getFirst_name(), contactDto.getLast_name(), contactDto.getPhone(), contactDto.isBotStarted());
                return contactDto;
            }
        }
        return null;
    }

    public void addNewContact(ContactDto contactDto) {
        List<ContactDto> contactDtos = JsonUtil.readFromJson(telegramConfig.getContactsPath());
        contactDtos.add(contactDto);
        JsonUtil.writeToJson(contactDtos, telegramConfig.getContactsPath());
        logger.info("Added new contact: ID={}, Username={}, FirstName={}, LastName={}, Phone={}, BotStarted={}",
                contactDto.getId(), contactDto.getUsername(), contactDto.getFirst_name(), contactDto.getLast_name(), contactDto.getPhone(), contactDto.isBotStarted());
    }

}
