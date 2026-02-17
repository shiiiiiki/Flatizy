package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import org.flatizy.flatizy.entity.NotificationType;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.UserNotificationSetting;
import org.flatizy.flatizy.repository.NotificationTypeRepository;
import org.flatizy.flatizy.repository.UserNotificationSettingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTypeRepository typeRepo;
    private final UserNotificationSettingRepository settingRepo;

    public List<NotificationType> getAllTypes() {
        return typeRepo.findAll();
    }

    public boolean isEnabled(User user, NotificationType type) {
        return settingRepo
                .findByUserAndNotificationType(user, type)
                .map(UserNotificationSetting::isEnabled)
                .orElse(true); // по умолчанию ВКЛ
    }

    public void toggle(User user, NotificationType type) {

        UserNotificationSetting setting =
                settingRepo.findByUserAndNotificationType(user, type)
                        .orElseGet(() -> {
                            UserNotificationSetting s = new UserNotificationSetting();
                            s.setUser(user);
                            s.setNotificationType(type);
                            s.setEnabled(true);
                            return s;
                        });

        setting.setEnabled(!setting.isEnabled());
        settingRepo.save(setting);
    }

    public Optional<NotificationType> getTypeById(Long id) {
        return typeRepo.findById(id);
    }
}
