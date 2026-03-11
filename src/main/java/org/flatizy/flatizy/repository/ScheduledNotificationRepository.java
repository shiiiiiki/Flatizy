package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.ScheduledNotification;
import org.flatizy.flatizy.entity.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledNotificationRepository extends JpaRepository<ScheduledNotification, Long> {
    List<ScheduledNotification> findByStatusAndScheduledAtBefore(NotificationStatus status, LocalDateTime now);
    Optional<ScheduledNotification> findByExternalId(String externalId);
    List<ScheduledNotification> findByStatus(NotificationStatus status);
}
