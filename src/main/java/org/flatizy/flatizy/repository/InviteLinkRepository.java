package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.InviteLink;
import org.flatizy.flatizy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InviteLinkRepository extends JpaRepository<InviteLink, Integer> {

    /**
     * Найти ссылку по коду
     */
    Optional<InviteLink> findByCode(String code);

    /**
     * Найти все активные ссылки созданные пользователем
     */
    List<InviteLink> findByCreatorAndActiveTrue(User creator);

    /**
     * Найти все ссылки созданные пользователем
     */
    List<InviteLink> findByCreatorOrderByCreatedAtDesc(User creator);

    /**
     * Найти все активные ссылки, срок которых истек
     */
    List<InviteLink> findByActiveTrueAndExpiresAtBefore(LocalDateTime dateTime);

    /**
     * Проверить существует ли активная ссылка с данным кодом
     */
    boolean existsByCodeAndActiveTrue(String code);

    /**
     * Получить статистику по ссылкам пользователя
     */
    @Query("SELECT COUNT(il) FROM InviteLink il WHERE il.creator = :creator AND il.active = true")
    long countActiveByCreator(User creator);

    @Query("SELECT SUM(il.usedCount) FROM InviteLink il WHERE il.creator = :creator")
    Long sumUsedCountByCreator(User creator);
}
