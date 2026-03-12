package org.flatizy.flatizy.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SecurityEventLogger {

    // Попытки входа
    public void logLoginAttempt(String email, String ip, boolean success) {
        if (success) {
            log.info("[SECURITY] LOGIN_SUCCESS | ip={} | email={}", ip, maskEmail(email));
        } else {
            log.warn("[SECURITY] LOGIN_FAILED | ip={} | email={}", ip, maskEmail(email));
        }
    }

    // Ошибки авторизации
    public void logAuthError(String ip, String uri, String reason) {
        log.warn("[SECURITY] AUTH_ERROR | ip={} | uri={} | reason={}", ip, uri, reason);
    }

    // Telegram события (БЕЗ содержимого сообщений)
    public void logTelegramEvent(String eventType, Long telegramId) {
        log.info("[TELEGRAM] {} | telegramId={}", eventType, telegramId);
    }

    // Подозрительные действия
    public void logSuspiciousActivity(String ip, String uri, String details) {
        log.warn("[SECURITY] SUSPICIOUS_ACTIVITY | ip={} | uri={} | details={}", ip, uri, details);
    }

    // Rate limit превышение
    public void logRateLimitExceeded(String ip, String uri, String endpointType) {
        log.warn("[SECURITY] RATE_LIMIT_EXCEEDED | ip={} | uri={} | type={}", ip, uri, endpointType);
    }

    // Webhook unauthorized
    public void logWebhookUnauthorized(String ip) {
        log.warn("[SECURITY] WEBHOOK_UNAUTHORIZED | ip={}", ip);
    }

    // Маскируем email для логов — не логируем персональные данные
    private String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}