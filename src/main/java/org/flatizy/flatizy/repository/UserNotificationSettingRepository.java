package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.NotificationType;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.UserNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserNotificationSettingRepository extends JpaRepository<UserNotificationSetting, Long> {

    Optional<UserNotificationSetting> findByUserAndNotificationType(User user, NotificationType type);

    @Query("SELECT u FROM User u WHERE NOT EXISTS (" +
            "SELECT 1 FROM UserNotificationSetting uns " +
            "WHERE uns.user = u AND uns.notificationType = :notificationType AND uns.enabled = false" +
            ") AND u.telegramId IS NOT NULL")
    List<User> findUsersWithNotificationEnabled(@Param("notificationType") NotificationType notificationType);
}
