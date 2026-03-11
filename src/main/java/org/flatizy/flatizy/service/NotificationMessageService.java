package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.config.TelegramBot;
import org.flatizy.flatizy.entity.NotificationType;
import org.flatizy.flatizy.entity.ScheduledNotification;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.dto.SendNotificationDto;
import org.flatizy.flatizy.entity.enums.NotificationStatus;
import org.flatizy.flatizy.repository.NotificationTypeRepository;
import org.flatizy.flatizy.repository.ScheduledNotificationRepository;
import org.flatizy.flatizy.repository.UserNotificationSettingRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationMessageService {

    private final ScheduledNotificationRepository scheduledNotificationRepo;
    private final NotificationTypeRepository notificationTypeRepo;
    private final UserNotificationSettingRepository userNotificationSettingRepo;
    private final TelegramBot telegramBot;

    /**
     * Отправка уведомления пользователям:
     * - Если sendNow = true, отправляется сразу
     * - Если sendNow = false, планируется на указанное время
     */
    public void sendNotification(SendNotificationDto dto) {
        Optional<NotificationType> notificationTypeOpt = notificationTypeRepo.findById(dto.getNotificationTypeId());

        if (notificationTypeOpt.isEmpty()) {
            log.error("Notification type with id {} not found", dto.getNotificationTypeId());
            throw new IllegalArgumentException("Notification type not found");
        }

        if (!dto.getSendNow() &&
                dto.getSendTime() != null &&
                dto.getSendTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ошибка даты отправки");
        }

        NotificationType notificationType = notificationTypeOpt.get();

        if (Boolean.TRUE.equals(dto.getSendNow())) {
            sendNotificationImmediately(notificationType, dto.getText());
        } else {
            scheduleNotification(notificationType, dto.getText(), dto.getSendTime());
        }
    }

    /**
     * Отправка уведомления сразу всем пользователям, у которых оно включено
     */
    @Async
    public void sendNotificationImmediately(NotificationType notificationType, String text) {
        List<User> users = userNotificationSettingRepo.findUsersWithNotificationEnabled(notificationType);

        log.info("Sending notification to {} users", users.size());

        String formattedMessage = String.format(
                "%s\n%s",
                notificationType.getTitle(),
                text
        );

        for (User user : users) {
            try {
                SendMessage message = new SendMessage();
                message.setChatId(user.getTelegramId());
                message.setText(formattedMessage);
                telegramBot.execute(message);
                log.debug("Notification sent to user {}", user.getId());
            } catch (TelegramApiException e) {
                log.error("Failed to send notification to user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    /**
     * Планирование уведомления на определенное время
     */
    public void scheduleNotification(NotificationType notificationType, String text, LocalDateTime scheduledAt) {
        ScheduledNotification notification = new ScheduledNotification();
        notification.setNotificationType(notificationType);
        notification.setText(text);
        notification.setScheduledAt(scheduledAt);
        notification.setStatus(NotificationStatus.SCHEDULED);
        notification.setSent(false);
        notification.setExternalId(UUID.randomUUID().toString());

        scheduledNotificationRepo.save(notification);
        log.info("Notification scheduled for {}", scheduledAt);
    }

    /**
     * Проверка и отправка планированных уведомлений (запускается каждую минуту)
     */
    //todo reduce to min before release
    @Scheduled(fixedDelay = 600000) // Каждую минуту
    public void processPendingNotifications() {
        List<ScheduledNotification> pending = scheduledNotificationRepo
                .findByStatusAndScheduledAtBefore(NotificationStatus.SCHEDULED, LocalDateTime.now());

        for (ScheduledNotification notification : pending) {
            try {
                sendNotificationImmediately(notification.getNotificationType(), notification.getText());
                notification.setStatus(NotificationStatus.SENT);
                notification.setSent(true);
                notification.setSentAt(LocalDateTime.now());
                scheduledNotificationRepo.save(notification);
                log.info("Scheduled notification {} sent successfully", notification.getId());
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.FAILED);
                scheduledNotificationRepo.save(notification);
                log.error("Failed to send scheduled notification {}: {}", notification.getId(), e.getMessage());
            }
        }
    }

    /**
     * Отмена планированного уведомления
     */
    public void cancelNotification(Long notificationId) {
        Optional<ScheduledNotification> notificationOpt = scheduledNotificationRepo.findById(notificationId);

        if (notificationOpt.isEmpty()) {
            throw new IllegalArgumentException("Notification not found");
        }

        ScheduledNotification notification = notificationOpt.get();

        if (notification.getStatus() == NotificationStatus.SCHEDULED) {
            notification.setStatus(NotificationStatus.CANCELLED);
            scheduledNotificationRepo.save(notification);
            log.info("Notification {} cancelled", notificationId);
        } else {
            throw new IllegalStateException("Cannot cancel notification with status " + notification.getStatus());
        }
    }
}
