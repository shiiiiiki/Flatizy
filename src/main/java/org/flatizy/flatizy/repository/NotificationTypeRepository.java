package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {
}
