package org.flatizy.flatizy.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.NotificationType;
import org.flatizy.flatizy.entity.ScheduledNotification;
import org.flatizy.flatizy.entity.dto.NotificationTypeDto;
import org.flatizy.flatizy.entity.dto.SendNotificationDto;
import org.flatizy.flatizy.entity.enums.NotificationStatus;
import org.flatizy.flatizy.repository.NotificationTypeRepository;
import org.flatizy.flatizy.repository.ScheduledNotificationRepository;
import org.flatizy.flatizy.service.NotificationMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationMessageService notificationMessageService;
    private final NotificationTypeRepository notificationTypeRepository;
    private final ScheduledNotificationRepository scheduledNotificationRepository;

    /**
     * Отправка уведомления пользователям
     * POST /api/notifications/send
     * Body: {
     *   "text": "Текст уведомления",
     *   "typeNotification": 1,
     *   "sendTime": "2026-02-21T15:30:00",
     *   "isSendNow": true
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody SendNotificationDto dto) {
        try {
            log.info("Received notification request: type={}, sendNow={}", dto.getNotificationTypeId(), dto.getSendNow());
            notificationMessageService.sendNotification(dto);
            return ResponseEntity.ok("Notification processed successfully");
        } catch (IllegalArgumentException e) {
            log.error("Invalid notification request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error sending notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending notification: " + e.getMessage());
        }
    }

    @GetMapping("/scheduled")
    public List<SendNotificationDto> getScheduled() {
        List<ScheduledNotification> messages = scheduledNotificationRepository.findAll();
        return messages.stream()
                .filter(msg-> msg.getStatus().equals(NotificationStatus.SCHEDULED))
                .map(n -> new SendNotificationDto(
                        n.getId(),
                        n.getText(),
                        n.getNotificationType().getId(),
                        n.getScheduledAt(),
                        false
                ))
                .toList();
    }

    /**
     * Получение всех типов уведомлений
     * GET /api/notifications/types
     */
    @GetMapping("/types")
    public ResponseEntity<List<NotificationTypeDto>> getNotificationTypes() {
        List<NotificationType> types = notificationTypeRepository.findAll();
        List<NotificationTypeDto> dtos = types.stream()
                .map(type -> new NotificationTypeDto(
                        type.getId(),
                        type.getCode().name(),
                        type.getTitle(),
                        type.getDescription()
                ))
                .collect(Collectors.toList());

        log.info("Returning {} notification types", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Отмена запланированного уведомления
     * DELETE /api/notifications/{notificationId}
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> cancelNotification(@PathVariable Long notificationId) {
        try {
            log.info("Cancelling notification with id {}", notificationId);
            notificationMessageService.cancelNotification(notificationId);
            return ResponseEntity.ok("Notification cancelled successfully");
        } catch (IllegalArgumentException e) {
            log.error("Notification not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Cannot cancel notification: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error cancelling notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cancelling notification: " + e.getMessage());
        }
    }
}
