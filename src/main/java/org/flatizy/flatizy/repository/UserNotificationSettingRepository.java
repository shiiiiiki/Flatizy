package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.NotificationType;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.UserNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {

    Optional<UserNotificationSetting> findByUserAndNotificationType(User user, NotificationType type);
}
