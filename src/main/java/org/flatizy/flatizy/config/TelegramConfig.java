package org.flatizy.flatizy.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

@Getter
@Configuration
public class TelegramConfig {
    private final String apiId;
    private final String apiHash;
    private final String botToken;
    private final String pythonPath;
    private final String pythonContactsScriptPath;
    private final String pythonAuthScriptPath;
    private final String contactsPath;

    public TelegramConfig() {
        Properties props = new Properties();
        try {
            props.load(TelegramConfig.class.getClassLoader().getResourceAsStream("application.properties"));
            this.apiId = props.getProperty("telegram.api.id");
            this.apiHash = props.getProperty("telegram.api.hash");
            this.botToken = props.getProperty("telegram.bot.token", "");
            this.pythonPath = props.getProperty("python.path", "python3");
            this.pythonContactsScriptPath = props.getProperty("python.script.contacts.path", "scripts/telegram_contacts.py");
            this.pythonAuthScriptPath = props.getProperty("python.script.auth.path", "scripts/telegram_auth.py");
            this.contactsPath = props.getProperty("telegram.contacts.path");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Telegram configuration", e);
        }
    }
}
