package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Integer> {
    Optional<UserSession> findByTelegramId(Long telegramId);
    List<UserSession> findByLastActivityBefore(LocalDateTime dateTime);
}
