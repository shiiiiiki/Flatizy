package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.UserSession;
import org.flatizy.flatizy.entity.enums.RequestType;
import org.flatizy.flatizy.entity.enums.UserSessionState;
import org.flatizy.flatizy.repository.UserSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    public UserSession getOrCreateSession(Long telegramId) {
        return userSessionRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    UserSession session = new UserSession();
                    session.setTelegramId(telegramId);
                    session.setState(UserSessionState.IDLE);
                    return userSessionRepository.save(session);
                });
    }

    public void updateState(Long telegramId, UserSessionState state) {
        UserSession session = getOrCreateSession(telegramId);
        session.setState(state);
        userSessionRepository.save(session);
    }

    public void setInviteCode(Long telegramId, String code) {
        UserSession session = getOrCreateSession(telegramId);
        session.setTempInviteCode(code);
        userSessionRepository.save(session);
    }

    public Optional<String> getInviteCode(Long telegramId) {
        return userSessionRepository.findByTelegramId(telegramId)
                .map(UserSession::getTempInviteCode);
    }

    public void clearSession(Long telegramId) {
        userSessionRepository.findByTelegramId(telegramId).ifPresent(session -> {
            session.setState(UserSessionState.IDLE);
            session.setTempInviteCode(null);
            session.setInviteMaxUses(null);
            session.setInviteExpirationDays(null);
            session.setInviteTargetRole(null);
            session.getSelectedApartmentIds().clear();
            userSessionRepository.save(session);
        });
    }

    public void addApartmentSelection(Long telegramId, Integer apartmentId) {
        UserSession session = getOrCreateSession(telegramId);
        if (!session.getSelectedApartmentIds().contains(apartmentId)) {
            session.getSelectedApartmentIds().add(apartmentId);
            userSessionRepository.save(session);
        }
    }

    public List<Integer> getSelectedApartments(Long telegramId) {
        return userSessionRepository.findByTelegramId(telegramId)
                .map(UserSession::getSelectedApartmentIds)
                .orElse(new java.util.ArrayList<>());
    }

    public void clearApartmentSelection(Long telegramId) {
        UserSession session = getOrCreateSession(telegramId);
        session.getSelectedApartmentIds().clear();
        userSessionRepository.save(session);
    }

    @Scheduled(cron = "0 0 */6 * * *") // Каждые 6 часов
    @Transactional
    public void cleanupInactiveSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<UserSession> inactive = userSessionRepository.findByLastActivityBefore(threshold);
        userSessionRepository.deleteAll(inactive);
        log.info("Удалено {} неактивных сессий", inactive.size());
    }

    public void updateSessionType(Long chatId, RequestType type) {
        UserSession session = getOrCreateSession(chatId);
        session.setRequestType(type);
        userSessionRepository.save(session);
    }
}
